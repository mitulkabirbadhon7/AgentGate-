package com.agentgate.invocation.application;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.domain.EndUser;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.agent.infrastructure.EndUserRepository;
import com.agentgate.approval.application.ApprovalService;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.health.CircuitBreakerHealthManager;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.guard.KillSwitchGuard;
import com.agentgate.invocation.guard.RedactionGuard;
import com.agentgate.invocation.guard.SchemaValidationGuard;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.observability.GatewayMetricsService;
import com.agentgate.outbox.application.OutboxService;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.quota.application.RateLimitService;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.adapter.ToolAdapter;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvocationService {

    private static final Logger log = LoggerFactory.getLogger(InvocationService.class);

    private final ToolInvocationRepository invocationRepository;
    private final AgentClientRepository agentRepository;
    private final EndUserRepository userRepository;
    private final ToolRepository toolRepository;
    private final SchemaValidationGuard schemaValidationGuard;
    private final KillSwitchGuard killSwitchGuard;
    private final PolicyService policyService;
    private final ApprovalService approvalService;
    private final RateLimitService rateLimitService;
    private final CircuitBreakerHealthManager healthManager;
    private final RedactionGuard redactionGuard;
    private final AuditChainService auditChainService;
    private final OutboxService outboxService;
    private final GatewayMetricsService metricsService;
    private final List<ToolAdapter> toolAdapters;
    private final ObjectMapper objectMapper;

    public InvocationService(
            ToolInvocationRepository invocationRepository,
            AgentClientRepository agentRepository,
            EndUserRepository userRepository,
            ToolRepository toolRepository,
            SchemaValidationGuard schemaValidationGuard,
            KillSwitchGuard killSwitchGuard,
            PolicyService policyService,
            @Lazy ApprovalService approvalService,
            RateLimitService rateLimitService,
            CircuitBreakerHealthManager healthManager,
            RedactionGuard redactionGuard,
            AuditChainService auditChainService,
            OutboxService outboxService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) GatewayMetricsService metricsService,
            List<ToolAdapter> toolAdapters,
            ObjectMapper objectMapper) {
        this.invocationRepository = invocationRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
        this.schemaValidationGuard = schemaValidationGuard;
        this.killSwitchGuard = killSwitchGuard;
        this.policyService = policyService;
        this.approvalService = approvalService;
        this.rateLimitService = rateLimitService;
        this.healthManager = healthManager;
        this.redactionGuard = redactionGuard;
        this.auditChainService = auditChainService;
        this.outboxService = outboxService;
        this.metricsService = metricsService;
        this.toolAdapters = toolAdapters;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = AgentGateException.class)
    public InvocationDTOs.InvocationResponse invokeTool(
            InvocationDTOs.InvokeToolRequest request,
            SecurityContextSnapshot securityContext) {

        // 1. Idempotency Check
        Optional<ToolInvocation> existing = invocationRepository.findByEventId(request.eventId());
        if (existing.isPresent()) {
            log.info("Idempotent invocation replay detected for eventId: {}", request.eventId());
            return InvocationDTOs.InvocationResponse.from(existing.get());
        }

        AgentClient agent = agentRepository.findById(securityContext.agentId())
                .orElseThrow(() -> new AgentGateException(ErrorCode.UNAUTHORIZED, "Authenticated agent not registered"));

        EndUser user = null;
        if (securityContext.userId() != null && !securityContext.userId().isBlank()) {
            user = userRepository.findById(securityContext.userId()).orElse(null);
        }

        Tool tool = toolRepository.findByName(request.toolName())
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found: " + request.toolName()));

        if (tool.getStatus() != Tool.ToolStatus.ACTIVE) {
            throw new AgentGateException(ErrorCode.FORBIDDEN, "Tool is not active: " + tool.getStatus());
        }

        // 2. Circuit Breaker check
        healthManager.verifyExecutionPermitted(tool);

        // 3. Kill Switch Guard (pre-check)
        killSwitchGuard.checkKillSwitch(agent.getClientId(), tool);

        // 4. Rate Limits & Quotas
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), tool, request.arguments());

        String rawArgsJson;
        try {
            rawArgsJson = objectMapper.writeValueAsString(request.arguments() != null ? request.arguments() : Map.of());
        } catch (Exception e) {
            rawArgsJson = "{}";
        }
        String redactedArgs = redactionGuard.redactSensitiveData(rawArgsJson);

        // 5. Initial Invocation State
        ToolInvocation invocation = new ToolInvocation(
                request.eventId(),
                agent,
                user,
                tool,
                redactedArgs,
                ToolInvocation.InvocationStatus.RECEIVED
        );
        invocation = invocationRepository.save(invocation);

        // 6. Audit INVOCATION_RECEIVED
        auditChainService.appendAuditEvent(invocation, "INVOCATION_RECEIVED", redactedArgs, agent.getClientId());

        // 7. Schema Validation Guard
        schemaValidationGuard.validateAndNormalize(tool, request.arguments());

        // 8. Policy Evaluation
        PolicyDTOs.EvaluationRequest evalReq = new PolicyDTOs.EvaluationRequest(
                agent.getClientId(),
                tool.getToolId(),
                request.arguments(),
                user != null ? user.getUserId() : null,
                securityContext.clearanceLevel(),
                false
        );

        PolicyDTOs.EvaluationResult evalResult = policyService.evaluate(evalReq);
        invocation.setPolicyVersionId(evalResult.versionId());

        if (evalResult.decision() == PolicyDTOs.Decision.DENY) {
            invocation.setStatus(ToolInvocation.InvocationStatus.BLOCKED);
            invocation.setErrorCode(ErrorCode.POLICY_DENIED.name());
            invocation.setCompletedAt(Instant.now());
            invocationRepository.save(invocation);

            auditChainService.appendAuditEvent(invocation, "BLOCKED", "{\"reason\":\"" + evalResult.reason() + "\"}", "policy_engine");
            outboxService.publishEvent("tool.invocation.blocked", "BLOCKED", redactedArgs, agent.getClientId());
            if (metricsService != null) {
                metricsService.recordInvocation(tool.getName(), agent.getClientId(), "BLOCKED", 0);
            }

            return InvocationDTOs.InvocationResponse.from(invocation);
        }

        if (evalResult.decision() == PolicyDTOs.Decision.REQUIRE_APPROVAL) {
            invocation.setStatus(ToolInvocation.InvocationStatus.RECEIVED);
            invocationRepository.save(invocation);

            approvalService.createApprovalRequest(invocation, rawArgsJson, 15);

            auditChainService.appendAuditEvent(invocation, "APPROVAL_PENDING", "{\"reason\":\"" + evalResult.reason() + "\"}", "policy_engine");
            outboxService.publishEvent("tool.invocation.approval_required", "APPROVAL_PENDING", redactedArgs, agent.getClientId());

            return InvocationDTOs.InvocationResponse.from(invocation);
        }

        // Re-check kill switch immediately before side effect (Security Invariant)
        killSwitchGuard.checkKillSwitch(agent.getClientId(), tool);

        // 9. Execution via Tool Adapter
        ToolAdapter adapter = toolAdapters.stream()
                .filter(a -> a.supports(tool))
                .findFirst()
                .orElseThrow(() -> new AgentGateException(ErrorCode.INTERNAL_ERROR, "No supported adapter found for tool server type: " + tool.getServer().getType()));

        long startExec = System.currentTimeMillis();
        try {
            String downstreamResult = adapter.execute(tool, request.arguments(), securityContext).join();
            long latency = System.currentTimeMillis() - startExec;

            healthManager.recordSuccess(tool);

            String sanitizedResult = redactionGuard.enforceTruncation(redactionGuard.redactSensitiveData(downstreamResult));

            invocation.setStatus(ToolInvocation.InvocationStatus.EXECUTED);
            invocation.setResponsePayloadRedacted(sanitizedResult);
            invocation.setLatencyMs((int) latency);
            invocation.setCompletedAt(Instant.now());
            invocationRepository.save(invocation);

            auditChainService.appendAuditEvent(invocation, "COMPLETED", sanitizedResult, agent.getClientId());
            outboxService.publishEvent("tool.invocation.completed", "COMPLETED", sanitizedResult, agent.getClientId());
            if (metricsService != null) {
                metricsService.recordInvocation(tool.getName(), agent.getClientId(), "COMPLETED", latency);
            }

            return InvocationDTOs.InvocationResponse.from(invocation);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startExec;
            healthManager.recordFailure(tool);

            boolean isWrite = isWriteOperation(tool);
            ToolInvocation.InvocationStatus failureStatus = isWrite
                    ? ToolInvocation.InvocationStatus.UNKNOWN_OUTCOME
                    : ToolInvocation.InvocationStatus.FAILED;

            invocation.setStatus(failureStatus);
            invocation.setErrorCode(ErrorCode.DOWNSTREAM_TIMEOUT.name());
            invocation.setLatencyMs((int) latency);
            invocation.setCompletedAt(Instant.now());
            invocationRepository.save(invocation);

            auditChainService.appendAuditEvent(invocation, failureStatus.name(), "{\"error\":\"" + e.getMessage() + "\"}", "downstream_adapter");
            outboxService.publishEvent("tool.invocation.failed", failureStatus.name(), "{\"error\":\"" + e.getMessage() + "\"}", agent.getClientId());
            if (metricsService != null) {
                metricsService.recordInvocation(tool.getName(), agent.getClientId(), failureStatus.name(), latency);
            }

            throw new AgentGateException(ErrorCode.DOWNSTREAM_TIMEOUT, "Downstream execution failed: " + e.getMessage(), e);
        }
    }

    private boolean isWriteOperation(Tool tool) {
        if (tool == null) return false;
        String hint = tool.getRoutingHint() != null ? tool.getRoutingHint().toUpperCase() : "";
        return hint.startsWith("POST") || hint.startsWith("PUT") || hint.startsWith("DELETE") || hint.startsWith("PATCH");
    }
}
