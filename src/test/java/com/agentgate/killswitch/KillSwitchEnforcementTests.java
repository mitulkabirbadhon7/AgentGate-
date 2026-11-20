package com.agentgate.killswitch;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.approval.application.ApprovalService;
import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.killswitch.application.KillSwitchDTOs;
import com.agentgate.killswitch.application.KillSwitchService;
import com.agentgate.killswitch.domain.KillSwitch;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KillSwitchEnforcementTests {

    @Autowired
    private KillSwitchService killSwitchService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalRequestRepository approvalRepository;

    @Autowired
    private ToolInvocationRepository invocationRepository;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    @Autowired
    private ToolRepository toolRepository;

    private AgentClient agent;
    private Tool tool;

    @BeforeEach
    void setUp() {
        agent = agentRepository.findById("ks-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("ks-agent-01", "KS Agent", "hash", "KS Agent")));

        DownstreamServer server = serverRepository.findByName("ks-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("ks-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 5)));

        tool = toolRepository.findByName("ks_tool_01")
                .orElseGet(() -> toolRepository.save(new Tool(
                        server,
                        "ks_tool_01",
                        "KS Tool",
                        "{\"type\":\"object\"}",
                        Tool.RiskLevel.HIGH,
                        "POST /write"
                )));
    }

    @Test
    void tripAgentKillSwitch_blocksExecutionAndCancelsPendingApprovals() {
        // Create an invocation and pending approval
        ToolInvocation inv = new ToolInvocation(
                UUID.randomUUID(), agent, null, tool, "{}", ToolInvocation.InvocationStatus.RECEIVED
        );
        inv = invocationRepository.save(inv);

        ApprovalRequest ar = approvalService.createApprovalRequest(inv, "{}", 10);
        assertThat(ar.getStatus()).isEqualTo(ApprovalRequest.ApprovalStatus.PENDING);

        // Trip the kill switch for this agent
        KillSwitchDTOs.KillSwitchResponse ks = killSwitchService.tripKillSwitch(new KillSwitchDTOs.TripKillSwitchRequest(
                KillSwitch.KillSwitchScope.AGENT,
                agent.getClientId(),
                "Suspicious agent activity detected",
                "admin-security"
        ));

        // Verify execution is blocked
        assertThat(killSwitchService.isExecutionBlocked(agent.getClientId(), tool)).isTrue();

        // Verify pending approval was auto-cancelled
        ApprovalRequest updatedAr = approvalRepository.findById(ar.getApprovalId()).orElseThrow();
        assertThat(updatedAr.getStatus()).isEqualTo(ApprovalRequest.ApprovalStatus.CANCELLED);
        assertThat(updatedAr.getDecisionReason()).contains("emergency kill switch trip");

        // Reset kill switch
        killSwitchService.resetKillSwitch(ks.killSwitchId(), "admin-security");
        assertThat(killSwitchService.isExecutionBlocked(agent.getClientId(), tool)).isFalse();
    }
}
