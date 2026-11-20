package com.agentgate.audit;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.audit.application.AuditDTOs;
import com.agentgate.audit.domain.AuditEvent;
import com.agentgate.audit.infrastructure.AuditEventRepository;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditChainIntegrityTests {

    @Autowired
    private AuditChainService auditChainService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ToolInvocationRepository invocationRepository;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    private ToolInvocation testInvocation;

    @BeforeEach
    void setUp() {
        AgentClient agent = agentRepository.findById("audit-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("audit-agent-01", "Audit Agent", "hash", "Audit Agent")));

        DownstreamServer server = serverRepository.findByName("audit-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("audit-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 5)));

        Tool tool = toolRepository.findByName("audit_tool_01")
                .orElseGet(() -> toolRepository.save(new Tool(server, "audit_tool_01", "Audit tool", "{\"type\":\"object\"}", Tool.RiskLevel.LOW, "POST /")));

        testInvocation = new ToolInvocation(
                UUID.randomUUID(),
                agent,
                null,
                tool,
                "{\"amount\":100}",
                ToolInvocation.InvocationStatus.RECEIVED
        );
        testInvocation = invocationRepository.save(testInvocation);
    }

    @Test
    void appendEvents_maintainsValidCryptographicChain() {
        auditChainService.appendAuditEvent(testInvocation, "INVOCATION_RECEIVED", "{\"step\":1}", "agent");
        auditChainService.appendAuditEvent(testInvocation, "POLICY_EVALUATED", "{\"step\":2}", "policy");
        auditChainService.appendAuditEvent(testInvocation, "COMPLETED", "{\"step\":3}", "agent");

        AuditDTOs.AuditVerificationReport report = auditChainService.verifyAuditIntegrity();
        assertThat(report.valid()).isTrue();
        assertThat(report.eventsChecked()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void whenPayloadTampered_verificationDetectsTampering() {
        AuditEvent event = auditChainService.appendAuditEvent(testInvocation, "INVOCATION_RECEIVED", "{\"secret\":\"original\"}", "agent");

        // Manually tamper with the persisted payload in DB
        event.setPayload("{\"secret\":\"hacked\"}");
        auditEventRepository.save(event);

        AuditDTOs.AuditVerificationReport report = auditChainService.verifyAuditIntegrity();
        assertThat(report.valid()).isFalse();
        assertThat(report.message()).contains("TAMPER DETECTED");
    }
}
