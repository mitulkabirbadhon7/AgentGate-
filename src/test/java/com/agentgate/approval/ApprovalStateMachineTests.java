package com.agentgate.approval;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.approval.application.ApprovalDTOs;
import com.agentgate.approval.application.ApprovalService;
import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApprovalStateMachineTests {

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

    private ToolInvocation testInvocation;

    @BeforeEach
    void setUp() {
        AgentClient agent = agentRepository.findById("approval-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("approval-agent-01", "Approval Agent", "hash", "Approval Agent")));

        DownstreamServer server = serverRepository.findByName("approval-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("approval-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 5)));

        Tool tool = toolRepository.findByName("approval_tool_01")
                .orElseGet(() -> toolRepository.save(new Tool(
                        server,
                        "approval_tool_01",
                        "Approval tool",
                        "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}",
                        Tool.RiskLevel.HIGH,
                        "POST /approve"
                )));

        testInvocation = new ToolInvocation(
                UUID.randomUUID(),
                agent,
                null,
                tool,
                "{\"amount\":5000}",
                ToolInvocation.InvocationStatus.RECEIVED
        );
        testInvocation = invocationRepository.save(testInvocation);
    }

    @Test
    void rejectPendingApproval_transitionsToRejected() {
        ApprovalRequest ar = approvalService.createApprovalRequest(testInvocation, "{\"amount\":5000}", 10);

        ApprovalDTOs.ApprovalResponse resp = approvalService.reject(
                ar.getApprovalId(),
                new ApprovalDTOs.RejectRequest("admin-bob", "Too risky")
        );

        assertThat(resp.status()).isEqualTo(ApprovalRequest.ApprovalStatus.REJECTED.name());
        assertThat(resp.decisionReason()).isEqualTo("Too risky");
    }

    @Test
    void terminalApproval_cannotBeApprovedOrRejectedAgain() {
        ApprovalRequest ar = approvalService.createApprovalRequest(testInvocation, "{\"amount\":5000}", 10);
        approvalService.reject(ar.getApprovalId(), new ApprovalDTOs.RejectRequest("admin-bob", "Rejected"));

        // Attempting to approve already-rejected request must throw CONFLICT
        assertThatThrownBy(() -> approvalService.approve(ar.getApprovalId(), new ApprovalDTOs.ApproveRequest("admin-alice", "Trying to approve")))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                });
    }

    @Test
    void editAndApprove_withInvalidSchema_throwsValidationException() {
        ApprovalRequest ar = approvalService.createApprovalRequest(testInvocation, "{\"amount\":5000}", 10);

        // Supply string for amount where number is required
        ApprovalDTOs.EditAndApproveRequest editReq = new ApprovalDTOs.EditAndApproveRequest(
                "admin-alice",
                Map.of("amount", "not-a-number"),
                "Changed amount to string"
        );

        assertThatThrownBy(() -> approvalService.editAndApprove(ar.getApprovalId(), editReq))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                });
    }
}
