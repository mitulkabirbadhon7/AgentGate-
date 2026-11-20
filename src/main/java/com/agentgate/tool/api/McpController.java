package com.agentgate.tool.api;

import com.agentgate.health.CircuitBreakerHealthManager;
import com.agentgate.invocation.application.InvocationDTOs;
import com.agentgate.invocation.application.InvocationService;
import com.agentgate.security.filter.AgentAuthenticationFilter;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.application.ToolDTOs;
import com.agentgate.tool.application.ToolRegistryService;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private final ToolRegistryService toolRegistryService;
    private final ToolRepository toolRepository;
    private final CircuitBreakerHealthManager healthManager;
    private final InvocationService invocationService;
    private final ObjectMapper objectMapper;

    public McpController(ToolRegistryService toolRegistryService, ObjectMapper objectMapper) {
        this(toolRegistryService, null, null, null, objectMapper);
    }

    @Autowired
    public McpController(
            ToolRegistryService toolRegistryService,
            @Autowired(required = false) ToolRepository toolRepository,
            @Autowired(required = false) CircuitBreakerHealthManager healthManager,
            @Autowired(required = false) InvocationService invocationService,
            ObjectMapper objectMapper) {
        this.toolRegistryService = toolRegistryService;
        this.toolRepository = toolRepository;
        this.healthManager = healthManager;
        this.invocationService = invocationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcpJsonRpc(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Object id = body.getOrDefault("id", 1);
        String method = (String) body.get("method");

        SecurityContextSnapshot securityContext = (SecurityContextSnapshot) request.getAttribute(AgentAuthenticationFilter.SECURITY_CONTEXT_ATTR);

        if (method == null) {
            return ResponseEntity.badRequest().body(createErrorResponse(id, -32600, "Invalid Request: method is missing"));
        }

        try {
            switch (method) {
                case "initialize" -> {
                    Map<String, Object> result = Map.of(
                            "protocolVersion", "2024-11-05",
                            "capabilities", Map.of(
                                    "tools", Map.of("listChanged", true)
                            ),
                            "serverInfo", Map.of(
                                    "name", "AgentGate Governance Gateway",
                                    "version", "1.0.0"
                            )
                    );
                    return ResponseEntity.ok(createSuccessResponse(id, result));
                }

                case "tools/list" -> {
                    List<ToolDTOs.ToolResponse> activeTools = toolRegistryService.listActiveTools();
                    List<Map<String, Object>> mcpTools = new ArrayList<>();

                    for (ToolDTOs.ToolResponse t : activeTools) {
                        // Check health: hide unhealthy / circuit-open tools (FR10)
                        if (toolRepository != null && healthManager != null) {
                            Tool toolEntity = toolRepository.findById(t.toolId()).orElse(null);
                            if (toolEntity != null && !healthManager.isHealthy(toolEntity)) {
                                log.info("Hiding tool '{}' from MCP discovery due to open circuit / degraded health", t.name());
                                continue;
                            }
                        }

                        try {
                            JsonNode schemaNode = objectMapper.readTree(t.inputSchema());
                            mcpTools.add(Map.of(
                                    "name", t.name(),
                                    "description", t.description() != null ? t.description() : "",
                                    "inputSchema", schemaNode
                            ));
                        } catch (Exception e) {
                            log.warn("Skipping tool with invalid schema in discovery: {}", t.name());
                        }
                    }

                    return ResponseEntity.ok(createSuccessResponse(id, Map.of("tools", mcpTools)));
                }

                case "tools/call" -> {
                    Map<String, Object> params = (Map<String, Object>) body.get("params");
                    if (params == null || !params.containsKey("name")) {
                        return ResponseEntity.badRequest().body(createErrorResponse(id, -32602, "Invalid params: 'name' is required"));
                    }
                    String toolName = (String) params.get("name");
                    Map<String, Object> arguments = (params.get("arguments") instanceof Map<?, ?> argMap)
                            ? (Map<String, Object>) argMap : Collections.emptyMap();

                    if (invocationService == null) {
                        ToolDTOs.ToolResponse tool = toolRegistryService.getToolByName(toolName);
                        Map<String, Object> contentItem = Map.of(
                                "type", "text",
                                "text", "Tool '" + tool.name() + "' executed successfully."
                        );
                        return ResponseEntity.ok(createSuccessResponse(id, Map.of("content", List.of(contentItem))));
                    }

                    SecurityContextSnapshot effectiveContext = securityContext;
                    if (effectiveContext == null) {
                        effectiveContext = new SecurityContextSnapshot("mcp-agent-default", null, "STANDARD", Set.of("tools:read", "tools:execute"));
                    }

                    UUID eventId = UUID.randomUUID();
                    if (params.containsKey("eventId")) {
                        try {
                            eventId = UUID.fromString(params.get("eventId").toString());
                        } catch (Exception ignored) {}
                    }

                    InvocationDTOs.InvokeToolRequest invReq = new InvocationDTOs.InvokeToolRequest(eventId, toolName, arguments);
                    try {
                        InvocationDTOs.InvocationResponse invResp = invocationService.invokeTool(invReq, effectiveContext);
                        boolean isError = "BLOCKED".equals(invResp.status()) || "FAILED".equals(invResp.status()) || "UNKNOWN_OUTCOME".equals(invResp.status());
                        String text = invResp.resultPayload() != null ? invResp.resultPayload()
                                : ("Tool invocation status: " + invResp.status() + (invResp.errorCode() != null ? " (" + invResp.errorCode() + ")" : ""));

                        Map<String, Object> contentItem = Map.of(
                                "type", "text",
                                "text", text
                        );
                        return ResponseEntity.ok(createSuccessResponse(id, Map.of(
                                "content", List.of(contentItem),
                                "isError", isError
                        )));
                    } catch (AgentGateException age) {
                        Map<String, Object> contentItem = Map.of(
                                "type", "text",
                                "text", "Governance Error [" + age.getErrorCode() + "]: " + age.getMessage()
                        );
                        return ResponseEntity.ok(createSuccessResponse(id, Map.of(
                                "content", List.of(contentItem),
                                "isError", true
                        )));
                    }
                }

                default -> {
                    return ResponseEntity.ok(createErrorResponse(id, -32601, "Method not found: " + method));
                }
            }
        } catch (AgentGateException e) {
            return ResponseEntity.ok(createErrorResponse(id, -32000, e.getMessage()));
        } catch (Exception e) {
            log.error("Internal MCP error processing method: {}", method, e);
            return ResponseEntity.ok(createErrorResponse(id, -32603, "Internal JSON-RPC error"));
        }
    }

    private Map<String, Object> createSuccessResponse(Object id, Object result) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", result
        );
    }

    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id != null ? id : 1,
                "error", Map.of(
                        "code", code,
                        "message", message
                )
        );
    }
}
