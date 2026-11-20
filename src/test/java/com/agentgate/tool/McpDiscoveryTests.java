package com.agentgate.tool;

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

import static org.assertj.core.api.Assertions.assertThat;
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
}
