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
import java.util.concurrent.CompletableFuture;

@Component
public class RestToolAdapter implements ToolAdapter {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RestToolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean supports(Tool tool) {
        return tool.getServer() != null && tool.getServer().getType() == DownstreamServer.ServerType.REST;
    }

    @Override
    public CompletableFuture<String> execute(Tool tool, Map<String, Object> arguments, SecurityContextSnapshot context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String baseEndpoint = tool.getServer().getEndpointUrl();
                String routingHint = tool.getRoutingHint() != null ? tool.getRoutingHint() : "POST /";
                String[] parts = routingHint.split("\\s+", 2);
                String method = parts[0].toUpperCase();
                String path = parts.length > 1 ? parts[1] : "";

                // Replace path variables e.g. /users/{id}
                String finalPath = path;
                if (arguments != null) {
                    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                        String placeholder = "{" + entry.getKey() + "}";
                        if (finalPath.contains(placeholder)) {
                            finalPath = finalPath.replace(placeholder, String.valueOf(entry.getValue()));
                        }
                    }
                }

                String fullUrl = baseEndpoint.replaceAll("/+$", "") + (finalPath.startsWith("/") ? finalPath : "/" + finalPath);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10));

                if ("GET".equals(method)) {
                    reqBuilder.GET();
                } else if ("DELETE".equals(method)) {
                    reqBuilder.DELETE();
                } else {
                    String jsonBody = (arguments != null && !arguments.isEmpty()) ? objectMapper.writeValueAsString(arguments) : "{}";
                    reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
                }

                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new AgentGateException(ErrorCode.SERVICE_UNAVAILABLE, "Downstream REST error: HTTP " + response.statusCode());
                }
                return response.body();
            } catch (AgentGateException e) {
                throw e;
            } catch (Exception e) {
                throw new AgentGateException(ErrorCode.DOWNSTREAM_TIMEOUT, "Downstream REST execution failed: " + e.getMessage(), e);
            }
        });
    }
}
