package com.agentgate.invocation.application;

import com.agentgate.invocation.domain.ToolInvocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class InvocationDTOs {

    public record InvokeToolRequest(
        UUID eventId,

        @NotBlank(message = "toolName is required")
        String toolName,

        Map<String, Object> arguments
    ) {
        public UUID effectiveEventId() {
            return eventId != null ? eventId : UUID.randomUUID();
        }
    }

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
                inv.getAgent() != null ? inv.getAgent().getClientId() : null,
                inv.getTool() != null ? inv.getTool().getName() : null,
                inv.getStatus() != null ? inv.getStatus().name() : null,
                inv.getErrorCode(),
                inv.getResponsePayloadRedacted(),
                inv.getLatencyMs(),
                inv.getCreatedAt(),
                inv.getCompletedAt()
            );
        }
    }
}
