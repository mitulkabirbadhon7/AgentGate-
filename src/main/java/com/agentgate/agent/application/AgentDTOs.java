package com.agentgate.agent.application;

import com.agentgate.agent.domain.AgentClient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class AgentDTOs {

    public record CreateAgentRequest(
        @NotBlank(message = "Agent client_id cannot be blank")
        @Size(max = 64)
        String clientId,

        @NotBlank(message = "Agent name cannot be blank")
        @Size(max = 128)
        String name,

        String displayName
    ) {}

    public record AgentResponse(
        String clientId,
        String name,
        String displayName,
        String status,
        Instant createdAt
    ) {
        public static AgentResponse from(AgentClient client) {
            return new AgentResponse(
                client.getClientId(),
                client.getName(),
                client.getDisplayName(),
                client.getStatus().name(),
                client.getCreatedAt()
            );
        }
    }

    public record SecretRotationResponse(
        String clientId,
        String clientSecret,
        String message
    ) {}
}
