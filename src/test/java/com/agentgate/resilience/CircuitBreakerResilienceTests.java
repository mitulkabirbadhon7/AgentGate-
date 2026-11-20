package com.agentgate.resilience;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.health.CircuitBreakerHealthManager;
import com.agentgate.invocation.application.InvocationDTOs;
import com.agentgate.invocation.application.InvocationService;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.adapter.RestToolAdapter;
import com.agentgate.tool.api.McpController;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.domain.Tool.RiskLevel;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class CircuitBreakerResilienceTests {

    @Autowired
    private CircuitBreakerHealthManager healthManager;

    @Autowired
    private McpController mcpController;

    @Autowired
    private InvocationService invocationService;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolInvocationRepository invocationRepository;

    @Autowired
    private PolicyService policyService;

    @MockBean
    private RestToolAdapter mockRestToolAdapter;

    private AgentClient agent;
    private DownstreamServer server;
    private Tool writeTool;

    @BeforeEach
    void setUp() {
        agent = agentRepository.findById("cb-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("cb-agent-01", "Circuit Breaker Agent", "hash", "CB Agent")));

        server = serverRepository.findByName("cb-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("cb-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 3)));

        writeTool = toolRepository.findByName("cb_write_tool")
                .orElseGet(() -> toolRepository.save(new Tool(server, "cb_write_tool", "Write Tool", "{\"type\":\"object\"}", RiskLevel.HIGH, "POST /api/write")));

        healthManager.reset(writeTool);

        // Bind allow policy for clean test execution
        try {
            var pol = policyService.createPolicy(new PolicyDTOs.CreatePolicyRequest("cb-allow-" + UUID.randomUUID().toString().substring(0, 8)));
            policyService.publishVersion(new PolicyDTOs.CreateVersionRequest(pol.policyId(), "{\"rules\":[{\"id\":\"r-cb\",\"effect\":\"ALLOW\",\"reason\":\"CB Allow\"}]}", "test-admin"));
            policyService.createBinding(new PolicyDTOs.CreateBindingRequest(pol.policyId(), agent.getClientId(), writeTool.getToolId(), PolicyBinding.BindingScope.AGENT_TOOL, 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Circuit breaker: trips to OPEN after threshold failures and rejects execution")
    void testCircuitTripsToOpenAfterThresholdFailures() {
        assertThat(healthManager.isHealthy(writeTool)).isTrue();
        assertThat(healthManager.getState(writeTool)).isEqualTo(CircuitBreakerHealthManager.CircuitState.CLOSED);

        // Threshold is 3 from server definition
        healthManager.recordFailure(writeTool);
        healthManager.recordFailure(writeTool);
        assertThat(healthManager.isHealthy(writeTool)).isTrue();

        // 3rd failure trips circuit
        healthManager.recordFailure(writeTool);
        assertThat(healthManager.getState(writeTool)).isEqualTo(CircuitBreakerHealthManager.CircuitState.OPEN);
        assertThat(healthManager.isHealthy(writeTool)).isFalse();

        // verify execution throws CIRCUIT_BREAKER_OPEN
        assertThatThrownBy(() -> healthManager.verifyExecutionPermitted(writeTool))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.CIRCUIT_BREAKER_OPEN);
                });
    }

    @Test
    @DisplayName("MCP Discovery: hides circuit-open tools from tools/list")
    void testCircuitBreakerHidesToolFromMcpDiscovery() {
        // When healthy, tool is returned in tools/list
        Map<String, Object> listRequest = Map.of("jsonrpc", "2.0", "id", "test-1", "method", "tools/list");
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        ResponseEntity<Map<String, Object>> response = mcpController.handleMcpJsonRpc(listRequest, httpReq);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        assertThat(tools).anyMatch(t -> "cb_write_tool".equals(t.get("name")));

        // Trip circuit to OPEN
        healthManager.forceState(writeTool, CircuitBreakerHealthManager.CircuitState.OPEN, Instant.now());
        assertThat(healthManager.isHealthy(writeTool)).isFalse();

        // tools/list must now omit the open circuit tool
        ResponseEntity<Map<String, Object>> responseAfterTrip = mcpController.handleMcpJsonRpc(listRequest, httpReq);
        Map<String, Object> resultAfterTrip = (Map<String, Object>) responseAfterTrip.getBody().get("result");
        List<Map<String, Object>> toolsAfterTrip = (List<Map<String, Object>>) resultAfterTrip.get("tools");
        assertThat(toolsAfterTrip).noneMatch(t -> "cb_write_tool".equals(t.get("name")));
    }

    @Test
    @DisplayName("Circuit recovery: transitions to HALF_OPEN after duration and resets to CLOSED on success")
    void testCircuitRecovery() {
        // Force OPEN 15 seconds ago (wait duration is 10s)
        Instant fifteenSecAgo = Instant.now().minusSeconds(15);
        healthManager.forceState(writeTool, CircuitBreakerHealthManager.CircuitState.OPEN, fifteenSecAgo);

        // isHealthy should return true and transition to HALF_OPEN
        assertThat(healthManager.isHealthy(writeTool)).isTrue();
        assertThat(healthManager.getState(writeTool)).isEqualTo(CircuitBreakerHealthManager.CircuitState.HALF_OPEN);

        // Success closes circuit
        healthManager.recordSuccess(writeTool);
        assertThat(healthManager.getState(writeTool)).isEqualTo(CircuitBreakerHealthManager.CircuitState.CLOSED);
    }

    @Test
    @DisplayName("Downstream timeout: write tool failure marks UNKNOWN_OUTCOME without blind retry")
    void testDownstreamWriteTimeoutRecordsUnknownOutcome() {
        when(mockRestToolAdapter.supports(any())).thenReturn(true);
        when(mockRestToolAdapter.execute(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("Gateway connection timeout")));

        SecurityContextSnapshot snapshot = new SecurityContextSnapshot(agent.getClientId(), "test-user", "STANDARD", Set.of("tools:read", "tools:execute"));
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> invocationService.invokeTool(
                new InvocationDTOs.InvokeToolRequest(eventId, "cb_write_tool", Map.of("key", "value")),
                snapshot
        )).isInstanceOf(AgentGateException.class);

        ToolInvocation invocation = invocationRepository.findByEventId(eventId).orElseThrow();
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocation.InvocationStatus.UNKNOWN_OUTCOME);
        assertThat(invocation.getErrorCode()).isEqualTo(ErrorCode.DOWNSTREAM_TIMEOUT.name());
    }
}
