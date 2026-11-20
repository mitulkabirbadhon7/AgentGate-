package com.agentgate.invocation;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.invocation.application.InvocationDTOs;
import com.agentgate.invocation.application.InvocationService;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InvocationLifecycleTests {

    @Autowired
    private InvocationService invocationService;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private PolicyService policyService;

    private AgentClient agent;
    private Tool tool;

    @BeforeEach
    void setUp() {
        agent = agentRepository.findById("lifecycle-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("lifecycle-agent-01", "Lifecycle Agent", "hash", "Lifecycle Agent")));

        DownstreamServer server = serverRepository.findByName("lifecycle-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("lifecycle-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 5)));

        tool = toolRepository.findByName("lifecycle_echo_tool")
                .orElseGet(() -> toolRepository.save(new Tool(
                        server,
                        "lifecycle_echo_tool",
                        "Lifecycle Echo Tool",
                        "{\"type\":\"object\"}",
                        Tool.RiskLevel.LOW,
                        "POST /echo"
                )));
    }

    @Test
    void defaultDeny_blocksInvocationAndLogsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        InvocationDTOs.InvokeToolRequest req = new InvocationDTOs.InvokeToolRequest(
                eventId,
                "lifecycle_echo_tool",
                Map.of("message", "hello")
        );

        SecurityContextSnapshot context = new SecurityContextSnapshot(agent.getClientId(), null, "STANDARD", Set.of("call"));
        InvocationDTOs.InvocationResponse response = invocationService.invokeTool(req, context);

        assertThat(response.status()).isEqualTo(ToolInvocation.InvocationStatus.BLOCKED.name());
        assertThat(response.errorCode()).isEqualTo("POLICY_DENIED");
    }

    @Test
    void idempotencyReplay_returnsExistingInvocationWithoutDuplicate() {
        UUID eventId = UUID.randomUUID();
        InvocationDTOs.InvokeToolRequest req = new InvocationDTOs.InvokeToolRequest(
                eventId,
                "lifecycle_echo_tool",
                Map.of("message", "hello")
        );

        SecurityContextSnapshot context = new SecurityContextSnapshot(agent.getClientId(), null, "STANDARD", Set.of("call"));

        InvocationDTOs.InvocationResponse resp1 = invocationService.invokeTool(req, context);
        InvocationDTOs.InvocationResponse resp2 = invocationService.invokeTool(req, context);

        assertThat(resp1.invocationId()).isEqualTo(resp2.invocationId());
        assertThat(resp1.eventId()).isEqualTo(resp2.eventId());
    }
}
