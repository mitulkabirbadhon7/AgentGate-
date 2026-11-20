package com.agentgate.tool.application;

import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class ToolDTOs {

    public record CreateServerRequest(
        @NotBlank(message = "Server name cannot be blank")
        @Size(max = 128)
        String name,

        @NotNull(message = "Server type is required")
        DownstreamServer.ServerType type,

        @NotBlank(message = "Endpoint URL cannot be blank")
        @Size(max = 512)
        String endpointUrl,

        int healthCheckIntervalSec,
        int circuitOpenThreshold
    ) {}

    public record ServerResponse(
        Long serverId,
        String name,
        String type,
        String endpointUrl,
        int healthCheckIntervalSec,
        int circuitOpenThreshold,
        Instant createdAt
    ) {
        public static ServerResponse from(DownstreamServer server) {
            return new ServerResponse(
                server.getServerId(),
                server.getName(),
                server.getType().name(),
                server.getEndpointUrl(),
                server.getHealthCheckIntervalSec(),
                server.getCircuitOpenThreshold(),
                server.getCreatedAt()
            );
        }
    }

    public record CreateToolRequest(
        @NotNull(message = "Server ID is required")
        Long serverId,

        @NotBlank(message = "Tool name cannot be blank")
        @Size(max = 128)
        String name,

        String description,

        @NotBlank(message = "Input schema cannot be blank")
        String inputSchema,

        Tool.RiskLevel riskLevel,
        String routingHint
    ) {}

    public record ToolResponse(
        Long toolId,
        Long serverId,
        String serverName,
        String name,
        String description,
        String inputSchema,
        String riskLevel,
        String routingHint,
        String status,
        Instant createdAt
    ) {
        public static ToolResponse from(Tool tool) {
            return new ToolResponse(
                tool.getToolId(),
                tool.getServer().getServerId(),
                tool.getServer().getName(),
                tool.getName(),
                tool.getDescription(),
                tool.getInputSchema(),
                tool.getRiskLevel().name(),
                tool.getRoutingHint(),
                tool.getStatus().name(),
                tool.getCreatedAt()
            );
        }
    }

    public record OpenApiImportRequest(
        String specUrl,
        String rawContent,
        @NotBlank(message = "Server name is required")
        String serverName,
        Tool.RiskLevel defaultRiskLevel
    ) {}

    public record OpenApiImportSummary(
        Long serverId,
        String serverName,
        int importedCount,
        List<String> toolNames
    ) {}
}
