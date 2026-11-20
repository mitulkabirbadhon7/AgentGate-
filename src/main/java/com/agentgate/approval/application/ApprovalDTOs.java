package com.agentgate.approval.application;

import com.agentgate.approval.domain.ApprovalRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public class ApprovalDTOs {

    public record ApprovalResponse(
        Long approvalId,
        Long invocationId,
        String agentId,
        String toolName,
        String status,
        String originalArguments,
        String editedArguments,
        String approverId,
        String decisionReason,
        String argumentsHash,
        int version,
        Instant expiresAt,
        Instant createdAt
    ) {
        public static ApprovalResponse from(ApprovalRequest ar) {
            return new ApprovalResponse(
                ar.getApprovalId(),
                ar.getInvocation().getInvocationId(),
                ar.getInvocation().getAgent().getClientId(),
                ar.getInvocation().getTool().getName(),
                ar.getStatus().name(),
                ar.getOriginalArgumentsEncrypted(),
                ar.getEditedArgumentsEncrypted(),
                ar.getApproverId(),
                ar.getDecisionReason(),
                ar.getArgumentsHash(),
                ar.getVersion(),
                ar.getExpiresAt(),
                ar.getCreatedAt()
            );
        }
    }

    public record ApproveRequest(
        @NotBlank(message = "Approver ID is required")
        String approverId,

        String reason
    ) {}

    public record EditAndApproveRequest(
        @NotBlank(message = "Approver ID is required")
        String approverId,

        @NotNull(message = "Edited arguments are required")
        Map<String, Object> editedArguments,

        String reason
    ) {}

    public record RejectRequest(
        @NotBlank(message = "Approver ID is required")
        String approverId,

        @NotBlank(message = "Rejection reason is required")
        String reason
    ) {}
}
