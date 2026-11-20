package com.agentgate.invocation.application;

import com.agentgate.invocation.domain.ToolInvocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class InvocationDTOs {

    public record InvokeToolRequest(
        @NotNull(message = "eventId is required for correlation and idempotency")
        UUID eventId,

        @NotBlank(message = "toolName is required")
        String toolName,

        Map<String, Object> arguments
    ) {}

    public record InvocationResponse(
        Long invocationId,
        UUID eventId,
        String agentId,
        String toolName,
        String status,
        String errorCode,
        String resultPayload,
        Integer latencyMs,
        Instant createdAt,
        Instant completedAt
    ) {
        public static InvocationResponse from(ToolInvocation inv) {
            return new InvocationResponse(
                inv.getInvocationId(),
                inv.getEventId(),
                inv.getAgent().getClientId(),
                inv.getTool().getName(),
                inv.getStatus().name(),
                inv.getErrorCode(),
                inv.getResponsePayloadRedacted(),
                inv.getLatencyMs(),
                inv.getCreatedAt(),
                inv.getCompletedAt()
            );
        }
    }
}
