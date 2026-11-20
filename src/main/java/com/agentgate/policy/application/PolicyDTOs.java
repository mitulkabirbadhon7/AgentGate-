package com.agentgate.policy.application;

import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public class PolicyDTOs {

    public record CreatePolicyRequest(
        @NotBlank(message = "Policy name is required")
        String name
    ) {}

    public record CreateVersionRequest(
        @NotNull(message = "Policy ID is required")
        Long policyId,

        @NotBlank(message = "Rules JSON is required")
        String rulesTree,

        @NotBlank(message = "Audit user ID is required")
        String auditByUser
    ) {}

    public record CreateBindingRequest(
        @NotNull(message = "Policy ID is required")
        Long policyId,

        String agentId,
        Long toolId,

        @NotNull(message = "Binding scope is required")
        PolicyBinding.BindingScope scope,

        int priority
    ) {}

    public record PolicyResponse(
        Long policyId,
        String name,
        String status,
        Long currentVersionId,
        boolean activeVersionLocks,
        Instant createdAt
    ) {
        public static PolicyResponse from(Policy policy) {
            return new PolicyResponse(
                policy.getPolicyId(),
                policy.getName(),
                policy.getStatus().name(),
                policy.getCurrentVersionId(),
                policy.isActiveVersionLocks(),
                policy.getCreatedAt()
            );
        }
    }

    public record PolicyVersionResponse(
        Long versionId,
        Long policyId,
        int versionNumber,
        String rulesTree,
        String auditByUser,
        String versionHash,
        Instant effectiveFrom
    ) {
        public static PolicyVersionResponse from(PolicyVersion pv) {
            return new PolicyVersionResponse(
                pv.getVersionId(),
                pv.getPolicy().getPolicyId(),
                pv.getVersionNumber(),
                pv.getRulesTree(),
                pv.getAuditByUser(),
                pv.getVersionHash(),
                pv.getEffectiveFrom()
            );
        }
    }

    public record PolicyBindingResponse(
        Long bindingId,
        Long policyId,
        String agentId,
        Long toolId,
        String scope,
        int priority
    ) {
        public static PolicyBindingResponse from(PolicyBinding pb) {
            return new PolicyBindingResponse(
                pb.getBindingId(),
                pb.getPolicy().getPolicyId(),
                pb.getAgent() != null ? pb.getAgent().getClientId() : null,
                pb.getTool() != null ? pb.getTool().getToolId() : null,
                pb.getScope().name(),
                pb.getPriority()
            );
        }
    }

    public enum Decision {
        ALLOW,
        DENY,
        REQUIRE_APPROVAL
    }

    public record EvaluationRequest(
        @NotBlank(message = "Agent ID is required")
        String agentId,

        @NotNull(message = "Tool ID is required")
        Long toolId,

        Map<String, Object> arguments,
        String userId,
        String clearanceLevel,
        boolean dryRun
    ) {}

    public record EvaluationResult(
        Decision decision,
        String reason,
        Long policyId,
        Long versionId,
        boolean dryRun,
        long latencyMs
    ) {}
}
