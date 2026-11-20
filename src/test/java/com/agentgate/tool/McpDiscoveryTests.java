package com.agentgate.tool;

import com.agentgate.invocation.application.InvocationDTOs;
import com.agentgate.invocation.application.InvocationService;
import com.agentgate.security.filter.AgentAuthenticationFilter;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.api.McpController;
import com.agentgate.tool.application.ToolDTOs;
import com.agentgate.tool.application.ToolRegistryService;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class McpDiscoveryTests {

    private McpController mcpController;
    private ToolRegistryService toolRegistryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        toolRegistryService = Mockito.mock(ToolRegistryService.class);
        mcpController = new McpController(toolRegistryService, objectMapper);
    }

    @Test
    void initialize_returnsValidProtocolCapabilities() {
        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", "init-1",
                "method", "initialize"
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        ResponseEntity<Map<String, Object>> response = mcpController.handleMcpJsonRpc(req, httpReq);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("id")).isEqualTo("init-1");

        Map<String, Object> result = (Map<String, Object>) body.get("result");
        assertThat(result.get("protocolVersion")).isEqualTo("2024-11-05");
        assertThat(result).containsKey("serverInfo");
    }

    @Test
    void toolsList_returnsOnlyActiveTools() {
        ToolDTOs.ToolResponse activeTool = new ToolDTOs.ToolResponse(
                1L, 10L, "refund-server", "process_refund", "Process customer refund",
                "{\"type\":\"object\"}", "HIGH", "POST /refund", "ACTIVE", Instant.now()
        );

        when(toolRegistryService.listActiveTools()).thenReturn(List.of(activeTool));

        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", "list-1",
                "method", "tools/list"
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        ResponseEntity<Map<String, Object>> response = mcpController.handleMcpJsonRpc(req, httpReq);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        Map<String, Object> result = (Map<String, Object>) body.get("result");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");

        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).get("name")).isEqualTo("process_refund");
    }

    @Test
    void toolsCall_withInvocationService_executesGovernanceAndReturnsMcpResult() {
        InvocationService mockInvocationService = Mockito.mock(InvocationService.class);
        McpController controllerWithGovernance = new McpController(
                toolRegistryService, null, null, mockInvocationService, objectMapper
        );

        InvocationDTOs.InvocationResponse successResp = new InvocationDTOs.InvocationResponse(
                100L, UUID.randomUUID(), "agent-1", "process_refund", "EXECUTED", null,
                "{\"status\":\"SUCCESS\",\"refundId\":\"rf-123\"}", 15, Instant.now(), Instant.now()
        );

        when(mockInvocationService.invokeTool(any(), any())).thenReturn(successResp);

        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", "call-1",
                "method", "tools/call",
                "params", Map.of("name", "process_refund", "arguments", Map.of("amount", 50))
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setAttribute(AgentAuthenticationFilter.SECURITY_CONTEXT_ATTR,
                new SecurityContextSnapshot("agent-1", null, "STANDARD", Set.of("call")));

        ResponseEntity<Map<String, Object>> response = controllerWithGovernance.handleMcpJsonRpc(req, httpReq);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        Map<String, Object> result = (Map<String, Object>) body.get("result");
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

        assertThat(content.get(0).get("text").toString()).contains("rf-123");
        assertThat(result.get("isError")).isEqualTo(false);
    }

    @Test
    void toolsCall_blockedByGovernance_returnsMcpErrorContent() {
        InvocationService mockInvocationService = Mockito.mock(InvocationService.class);
        McpController controllerWithGovernance = new McpController(
                toolRegistryService, null, null, mockInvocationService, objectMapper
        );

        when(mockInvocationService.invokeTool(any(), any()))
                .thenThrow(new AgentGateException(ErrorCode.POLICY_DENIED, "Policy denies action"));

        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "id", "call-2",
                "method", "tools/call",
                "params", Map.of("name", "process_refund", "arguments", Map.of("amount", 999999))
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        ResponseEntity<Map<String, Object>> response = controllerWithGovernance.handleMcpJsonRpc(req, httpReq);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        Map<String, Object> result = (Map<String, Object>) body.get("result");
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

        assertThat(content.get(0).get("text").toString()).contains("POLICY_DENIED");
        assertThat(result.get("isError")).isEqualTo(true);
    }
}
