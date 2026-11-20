package com.agentgate.tool.application;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class OpenApiImportService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiImportService.class);
    private final DownstreamServerRepository serverRepository;
    private final ToolRepository toolRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenApiImportService(
            DownstreamServerRepository serverRepository,
            ToolRepository toolRepository,
            ObjectMapper objectMapper) {
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public ToolDTOs.OpenApiImportSummary importOpenApi(ToolDTOs.OpenApiImportRequest request) {
        String specJson;
        String baseUrl = "http://localhost:8080";

        if (request.rawContent() != null && !request.rawContent().isBlank()) {
            specJson = request.rawContent().trim();
        } else if (request.specUrl() != null && !request.specUrl().isBlank()) {
            validateSsrfSafeUrl(request.specUrl());
            specJson = fetchSpecFromUrl(request.specUrl());
            try {
                URI uri = URI.create(request.specUrl());
                baseUrl = uri.getScheme() + "://" + uri.getAuthority();
            } catch (Exception ignored) {}
        } else {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Either specUrl or rawContent must be provided");
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(specJson);
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Failed to parse OpenAPI JSON specification: " + e.getMessage());
        }

        // Extract base server URL if defined in spec
        if (rootNode.has("servers") && rootNode.get("servers").isArray() && !rootNode.get("servers").isEmpty()) {
            JsonNode firstServer = rootNode.get("servers").get(0);
            if (firstServer.has("url")) {
                String specBase = firstServer.get("url").asText();
                if (specBase.startsWith("http://") || specBase.startsWith("https://")) {
                    baseUrl = specBase;
                }
            }
        }

        final String resolvedBaseUrl = baseUrl;
        DownstreamServer server = serverRepository.findByName(request.serverName())
                .orElseGet(() -> serverRepository.save(new DownstreamServer(
                        request.serverName(),
                        DownstreamServer.ServerType.REST,
                        resolvedBaseUrl,
                        30,
                        5
                )));

        List<String> importedTools = new ArrayList<>();
        JsonNode pathsNode = rootNode.get("paths");
        if (pathsNode != null && pathsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
            while (pathIterator.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                String path = pathEntry.getKey();
                JsonNode methodsNode = pathEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> methodIterator = methodsNode.fields();
                while (methodIterator.hasNext()) {
                    Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
                    String method = methodEntry.getKey().toLowerCase();
                    if (!List.of("get", "post", "put", "delete", "patch").contains(method)) {
                        continue;
                    }

                    JsonNode operationNode = methodEntry.getValue();
                    String operationId = operationNode.has("operationId")
                            ? operationNode.get("operationId").asText()
                            : (method + "_" + path.replaceAll("[^a-zA-Z0-9]", "_")).replaceAll("_+", "_");

                    // Ensure unique name
                    String toolName = sanitizeToolName(operationId);
                    if (toolRepository.existsByName(toolName)) {
                        toolName = toolName + "_" + UUID.randomUUID().toString().substring(0, 6);
                    }

                    String summary = operationNode.has("summary") ? operationNode.get("summary").asText() : "";
                    String description = operationNode.has("description") ? operationNode.get("description").asText() : summary;

                    String inputSchema = buildInputSchema(operationNode);
                    Tool.RiskLevel risk = request.defaultRiskLevel() != null ? request.defaultRiskLevel() : Tool.RiskLevel.LOW;

                    Tool tool = new Tool(
                            server,
                            toolName,
                            description,
                            inputSchema,
                            risk,
                            method.toUpperCase() + " " + path
                    );
                    toolRepository.save(tool);
                    importedTools.add(toolName);
                }
            }
        }

        return new ToolDTOs.OpenApiImportSummary(server.getServerId(), server.getName(), importedTools.size(), importedTools);
    }

    public void validateSsrfSafeUrl(String urlString) {
        URI uri;
        try {
            uri = URI.create(urlString);
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Invalid OpenAPI spec URL format: " + urlString);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new AgentGateException(ErrorCode.FORBIDDEN, "SSRF Defense: Only http and https protocols are permitted");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "URL host cannot be empty");
        }

        // Cloud metadata and localhost names
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") ||
            lowerHost.equals("127.0.0.1") ||
            lowerHost.equals("::1") ||
            lowerHost.contains("metadata.google.internal") ||
            lowerHost.contains("169.254.169.254") ||
            lowerHost.endsWith(".internal") ||
            lowerHost.endsWith(".local")) {
            throw new AgentGateException(ErrorCode.FORBIDDEN, "SSRF Defense: Access to local, internal, or cloud metadata endpoints is prohibited");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() ||
                    address.isLoopbackAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isSiteLocalAddress() ||
                    isPrivateIpAddress(address)) {
                    throw new AgentGateException(ErrorCode.FORBIDDEN,
                            "SSRF Defense: Host resolved to prohibited private/internal IP address: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "SSRF Defense: Host DNS resolution failed for " + host);
        }
    }

    private boolean isPrivateIpAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) { // IPv4
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16
            if (b0 == 169 && b1 == 254) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
        }
        return false;
    }

    private String fetchSpecFromUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Failed to fetch OpenAPI spec: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (AgentGateException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Error downloading OpenAPI specification: " + e.getMessage());
        }
    }

    private String sanitizeToolName(String name) {
        String clean = name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
        if (clean.startsWith("_")) clean = clean.substring(1);
        if (clean.endsWith("_")) clean = clean.substring(0, clean.length() - 1);
        return clean.isEmpty() ? "tool_" + UUID.randomUUID().toString().substring(0, 8) : clean;
    }

    private String buildInputSchema(JsonNode operationNode) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        List<String> required = new ArrayList<>();

        if (operationNode.has("parameters") && operationNode.get("parameters").isArray()) {
            for (JsonNode param : operationNode.get("parameters")) {
                String paramName = param.has("name") ? param.get("name").asText() : "param";
                ObjectNode paramProp = objectMapper.createObjectNode();
                paramProp.put("type", param.has("schema") && param.get("schema").has("type")
                        ? param.get("schema").get("type").asText() : "string");
                if (param.has("description")) {
                    paramProp.put("description", param.get("description").asText());
                }
                properties.set(paramName, paramProp);
                if (param.has("required") && param.get("required").asBoolean()) {
                    required.add(paramName);
                }
            }
        }

        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.putPOJO("required", required);
        }
        return schema.toString();
    }
}
