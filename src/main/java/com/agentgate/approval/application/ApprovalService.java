package com.agentgate.approval.application;

import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.guard.RedactionGuard;
import com.agentgate.invocation.guard.SchemaValidationGuard;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.killswitch.application.KillSwitchService;
import com.agentgate.outbox.application.OutboxService;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.adapter.ToolAdapter;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRequestRepository approvalRepository;
    private final ToolInvocationRepository invocationRepository;
    private final SchemaValidationGuard schemaValidationGuard;
    private final RedactionGuard redactionGuard;
    private final PolicyService policyService;
    private final KillSwitchService killSwitchService;
    private final AuditChainService auditChainService;
    private final OutboxService outboxService;
    private final List<ToolAdapter> toolAdapters;
    private final ObjectMapper objectMapper;

    public ApprovalService(
            ApprovalRequestRepository approvalRepository,
            ToolInvocationRepository invocationRepository,
            SchemaValidationGuard schemaValidationGuard,
            RedactionGuard redactionGuard,
            PolicyService policyService,
            KillSwitchService killSwitchService,
            AuditChainService auditChainService,
            OutboxService outboxService,
            List<ToolAdapter> toolAdapters,
            ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.invocationRepository = invocationRepository;
        this.schemaValidationGuard = schemaValidationGuard;
        this.redactionGuard = redactionGuard;
        this.policyService = policyService;
        this.killSwitchService = killSwitchService;
        this.auditChainService = auditChainService;
        this.outboxService = outboxService;
        this.toolAdapters = toolAdapters;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApprovalRequest createApprovalRequest(ToolInvocation invocation, String rawArgsJson, long expiryMinutes) {
        String argsHash = computeSha256(rawArgsJson);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expiryMinutes > 0 ? expiryMinutes : 15));

        ApprovalRequest ar = new ApprovalRequest(invocation, rawArgsJson, argsHash, expiresAt);
        return approvalRepository.save(ar);
    }

    @Transactional
    public ApprovalDTOs.ApprovalResponse approve(Long approvalId, ApprovalDTOs.ApproveRequest request) {
        ApprovalRequest ar = findPendingApproval(approvalId);

        ar.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        ar.setApproverId(request.approverId());
        ar.setDecisionReason(request.reason());
        ar.setUpdatedAt(Instant.now());
        ar = approvalRepository.save(ar);

        ToolInvocation invocation = ar.getInvocation();
        executeApprovedInvocation(invocation, ar.getOriginalArgumentsEncrypted(), request.approverId());

        return ApprovalDTOs.ApprovalResponse.from(ar);
    }

    @Transactional
    public ApprovalDTOs.ApprovalResponse editAndApprove(Long approvalId, ApprovalDTOs.EditAndApproveRequest request) {
        ApprovalRequest ar = findPendingApproval(approvalId);

        String editedJson;
        try {
            editedJson = objectMapper.writeValueAsString(request.editedArguments());
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Failed to serialize edited arguments");
        }

        ToolInvocation invocation = ar.getInvocation();
        Tool tool = invocation.getTool();

        // REVALIDATION INVARIANTS (ADR-0006)
        // 1. Re-validate JSON Schema
        schemaValidationGuard.validateAndNormalize(tool, request.editedArguments());

        // 2. Re-check Kill Switch
        if (killSwitchService.isExecutionBlocked(invocation.getAgent().getClientId(), tool)) {
            throw new AgentGateException(ErrorCode.KILL_SWITCH_ACTIVE, "Cannot approve edited arguments: Kill switch is active for tool/agent");
        }

        // 3. Re-evaluate Policy
        PolicyDTOs.EvaluationResult eval = policyService.evaluate(new PolicyDTOs.EvaluationRequest(
                invocation.getAgent().getClientId(),
                tool.getToolId(),
                request.editedArguments(),
                invocation.getUser() != null ? invocation.getUser().getUserId() : null,
                "ADMIN",
                false
        ));
        if (eval.decision() == PolicyDTOs.Decision.DENY) {
            throw new AgentGateException(ErrorCode.POLICY_DENIED, "Edited arguments violate policy: " + eval.reason());
        }

        ar.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        ar.setEditedArgumentsEncrypted(editedJson);
        ar.setApproverId(request.approverId());
        ar.setDecisionReason(request.reason() != null ? request.reason() : "Approved with modified arguments");
        ar.setUpdatedAt(Instant.now());
        ar = approvalRepository.save(ar);

        executeApprovedInvocation(invocation, editedJson, request.approverId());

        return ApprovalDTOs.ApprovalResponse.from(ar);
    }

    @Transactional
    public ApprovalDTOs.ApprovalResponse reject(Long approvalId, ApprovalDTOs.RejectRequest request) {
        ApprovalRequest ar = findPendingApproval(approvalId);

        ar.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        ar.setApproverId(request.approverId());
        ar.setDecisionReason(request.reason());
        ar.setUpdatedAt(Instant.now());
        ar = approvalRepository.save(ar);

        ToolInvocation invocation = ar.getInvocation();
        invocation.setStatus(ToolInvocation.InvocationStatus.REJECTED);
        invocation.setErrorCode(ErrorCode.FORBIDDEN.name());
        invocation.setCompletedAt(Instant.now());
        invocationRepository.save(invocation);

        auditChainService.appendAuditEvent(invocation, "REJECTED", "{\"reason\":\"" + request.reason() + "\"}", request.approverId());
        outboxService.publishEvent("tool.invocation.rejected", "REJECTED", "{\"reason\":\"" + request.reason() + "\"}", invocation.getAgent().getClientId());

        return ApprovalDTOs.ApprovalResponse.from(ar);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDTOs.ApprovalResponse> listPendingApprovals() {
        return approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).stream()
                .map(ApprovalDTOs.ApprovalResponse::from)
                .toList();
    }

    private ApprovalRequest findPendingApproval(Long approvalId) {
        ApprovalRequest ar = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Approval request not found: " + approvalId));

        if (ar.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Illegal approval state transition: approval is already in terminal state " + ar.getStatus());
        }

        if (Instant.now().isAfter(ar.getExpiresAt())) {
            ar.setStatus(ApprovalRequest.ApprovalStatus.EXPIRED);
            ar.setDecisionReason("Approval window expired");
            approvalRepository.save(ar);

            ToolInvocation inv = ar.getInvocation();
            inv.setStatus(ToolInvocation.InvocationStatus.FAILED);
            inv.setErrorCode(ErrorCode.APPROVAL_EXPIRED.name());
            invocationRepository.save(inv);

            throw new AgentGateException(ErrorCode.APPROVAL_EXPIRED, "Approval request has expired and can no longer be processed");
        }

        return ar;
    }

    private void executeApprovedInvocation(ToolInvocation invocation, String argsJson, String approverId) {
        Tool tool = invocation.getTool();
        ToolAdapter adapter = toolAdapters.stream()
                .filter(a -> a.supports(tool))
                .findFirst()
                .orElseThrow(() -> new AgentGateException(ErrorCode.INTERNAL_ERROR, "No supported adapter found"));

        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {});
            long start = System.currentTimeMillis();
            SecurityContextSnapshot securityContext = new SecurityContextSnapshot(
                    approverId != null ? approverId : "admin_user",
                    null,
                    "ADMIN",
                    Set.of("tools:read", "tools:execute", "admin")
            );
            String result = adapter.execute(tool, args, securityContext).join();
            long latency = System.currentTimeMillis() - start;

            String sanitized = redactionGuard.enforceTruncation(redactionGuard.redactSensitiveData(result));

            invocation.setStatus(ToolInvocation.InvocationStatus.EXECUTED);
            invocation.setResponsePayloadRedacted(sanitized);
            invocation.setLatencyMs((int) latency);
            invocation.setCompletedAt(Instant.now());
            invocationRepository.save(invocation);

            auditChainService.appendAuditEvent(invocation, "APPROVED_AND_EXECUTED", sanitized, approverId);
            outboxService.publishEvent("tool.invocation.approved", "APPROVED", sanitized, invocation.getAgent().getClientId());
        } catch (Exception e) {
            log.error("Failed downstream execution for approved invocation: {}", invocation.getInvocationId(), e);
            boolean isWrite = isWriteOperation(tool);
            ToolInvocation.InvocationStatus failureStatus = isWrite
                    ? ToolInvocation.InvocationStatus.UNKNOWN_OUTCOME
                    : ToolInvocation.InvocationStatus.FAILED;

            invocation.setStatus(failureStatus);
            invocation.setErrorCode(ErrorCode.DOWNSTREAM_TIMEOUT.name());
            invocation.setCompletedAt(Instant.now());
            invocationRepository.save(invocation);

            auditChainService.appendAuditEvent(invocation, failureStatus.name(), "{\"error\":\"" + e.getMessage() + "\"}", approverId);
            outboxService.publishEvent("tool.invocation.failed", failureStatus.name(), "{\"error\":\"" + e.getMessage() + "\"}", invocation.getAgent().getClientId());
        }
    }

    private boolean isWriteOperation(Tool tool) {
        if (tool == null) return false;
        String hint = tool.getRoutingHint() != null ? tool.getRoutingHint().toUpperCase() : "";
        return hint.startsWith("POST") || hint.startsWith("PUT") || hint.startsWith("DELETE") || hint.startsWith("PATCH");
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256", e);
        }
    }
}
