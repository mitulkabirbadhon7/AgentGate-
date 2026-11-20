package com.agentgate.tool.adapter;

import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class McpToolAdapter implements ToolAdapter {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public McpToolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean supports(Tool tool) {
        return tool.getServer() != null && tool.getServer().getType() == DownstreamServer.ServerType.MCP;
    }

    @Override
    public CompletableFuture<String> execute(Tool tool, Map<String, Object> arguments, SecurityContextSnapshot context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = tool.getServer().getEndpointUrl();
                Map<String, Object> mcpRequest = Map.of(
                        "jsonrpc", "2.0",
                        "id", UUID.randomUUID().toString(),
                        "method", "tools/call",
                        "params", Map.of(
                                "name", tool.getName(),
                                "arguments", arguments
                        )
                );

                String requestBody = objectMapper.writeValueAsString(mcpRequest);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new AgentGateException(ErrorCode.SERVICE_UNAVAILABLE, "Downstream MCP error: HTTP " + response.statusCode());
                }
                return response.body();
            } catch (AgentGateException e) {
                throw e;
            } catch (Exception e) {
                throw new AgentGateException(ErrorCode.DOWNSTREAM_TIMEOUT, "Downstream MCP execution failed: " + e.getMessage(), e);
            }
        });
    }
}
