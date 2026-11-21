package com.agentgate.acceptance;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.killswitch.domain.KillSwitch;
import com.agentgate.killswitch.infrastructure.KillSwitchRepository;
import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.security.token.TokenService;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminWebRoutesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyVersionRepository policyVersionRepository;

    @Autowired
    private PolicyBindingRepository policyBindingRepository;

    @Autowired
    private ToolInvocationRepository invocationRepository;

    @Autowired
    private ApprovalRequestRepository approvalRepository;

    @Autowired
    private KillSwitchRepository killSwitchRepository;

    @Autowired
    private AuditChainService auditChainService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String validToken;

    @BeforeEach
    void seedTestData() {
        // 1. Ensure Agent
        AgentClient agent = agentRepository.findById("test-web-agent")
                .orElseGet(() -> agentRepository.save(new AgentClient(
                        "test-web-agent",
                        "Web Integration Test Agent",
                        passwordEncoder.encode("secret"),
                        "Display Name Test"
                )));

        // 2. Ensure Server
        DownstreamServer server = serverRepository.findByName("test-web-server")
                .orElseGet(() -> serverRepository.save(new DownstreamServer(
                        "test-web-server",
                        DownstreamServer.ServerType.REST,
                        "http://localhost:8080/api/v1/mock",
                        30,
                        5
                )));

        // 3. Ensure Tool
        Tool tool = toolRepository.findByName("test-web-tool")
                .orElseGet(() -> toolRepository.save(new Tool(
                        server,
                        "test-web-tool",
                        "Test description",
                        "{\"type\":\"object\"}",
                        Tool.RiskLevel.LOW,
                        "POST /mock/echo"
                )));

        // 4. Ensure Policy
        Policy policy = policyRepository.findByName("test-web-policy")
                .orElseGet(() -> {
                    Policy p = policyRepository.save(new Policy("test-web-policy"));
                    PolicyVersion pv = policyVersionRepository.save(new PolicyVersion(
                            p,
                            1,
                            "{\"rules\":[{\"id\":\"r1\",\"effect\":\"ALLOW\",\"reason\":\"ok\"}]}",
                            "test_admin",
                            "hash123"
                    ));
                    p.setCurrentVersionId(pv.getVersionId());
                    policyRepository.save(p);

                    policyBindingRepository.save(new PolicyBinding(
                            p, agent, null, PolicyBinding.BindingScope.AGENT, 100
                    ));
                    return p;
                });

        // 5. Ensure ToolInvocation & Approval
        UUID eventId = UUID.randomUUID();
        ToolInvocation inv = invocationRepository.save(new ToolInvocation(
                eventId,
                agent,
                null,
                tool,
                "{\"param\":\"value\"}",
                ToolInvocation.InvocationStatus.RECEIVED
        ));

        if (approvalRepository.count() == 0) {
            approvalRepository.save(new ApprovalRequest(
                    inv,
                    "{\"param\":\"value\"}",
                    "dummy_hash",
                    Instant.now().plusSeconds(600)
            ));
        }

        // 6. Ensure KillSwitch
        if (killSwitchRepository.count() == 0) {
            killSwitchRepository.save(new KillSwitch(
                    KillSwitch.KillSwitchScope.TOOL,
                    tool.getToolId().toString(),
                    "Unit test safety switch",
                    "admin_user"
            ));
        }

        // 7. Audit event
        auditChainService.appendAuditEvent(inv, "TEST_EVENT", "{\"test\":true}", "test_actor");

        // 8. Generate Bearer Token
        validToken = "Bearer " + tokenService.generateToken("test-web-agent", null, Set.of("tools:read", "tools:call", "admin:all"), 3600);
    }

    @AfterEach
    void tearDown() {
        killSwitchRepository.deleteAll();
    }

    @Test
    @DisplayName("Admin dashboard page renders successfully")
    void testDashboardRoute() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Gateway Overview")));
    }

    @Test
    @DisplayName("Tools and downstream servers page renders without SpEL errors")
    void testToolsRoute() throws Exception {
        mockMvc.perform(get("/tools"))
                .andExpect(status().isOk())
                .andExpect(view().name("tools"))
                .andExpect(content().string(containsString("test-web-server")))
                .andExpect(content().string(containsString("test-web-tool")));
    }

    @Test
    @DisplayName("Agents registry page renders without SpEL errors")
    void testAgentsRoute() throws Exception {
        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(view().name("agents"))
                .andExpect(content().string(containsString("test-web-agent")));
    }

    @Test
    @DisplayName("Policies and playground page renders successfully")
    void testPoliciesRoute() throws Exception {
        mockMvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(view().name("policies"))
                .andExpect(content().string(containsString("test-web-policy")));
    }

    @Test
    @DisplayName("Human approvals page renders without SpEL errors")
    void testApprovalsRoute() throws Exception {
        mockMvc.perform(get("/approvals"))
                .andExpect(status().isOk())
                .andExpect(view().name("approvals"))
                .andExpect(content().string(containsString("Pending Human Review Queue")));
    }

    @Test
    @DisplayName("Cryptographic audit chain page renders and integrity verification works")
    void testAuditRoute() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit"))
                .andExpect(content().string(containsString("Tamper-Evident SHA-256 Audit Chain")));

        mockMvc.perform(get("/audit").param("verify", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit"))
                .andExpect(content().string(containsString("CRYPTOGRAPHIC AUDIT VERIFICATION PASSED")));
    }

    @Test
    @DisplayName("Emergency kill switches page renders successfully")
    void testKillSwitchesRoute() throws Exception {
        mockMvc.perform(get("/killswitches"))
                .andExpect(status().isOk())
                .andExpect(view().name("killswitches"))
                .andExpect(content().string(containsString("Emergency Kill Switches")));
    }

    @Test
    @DisplayName("Kill switches toggle POST action works via form")
    void testKillSwitchToggle() throws Exception {
        mockMvc.perform(post("/killswitches/toggle")
                        .param("scope", "AGENT")
                        .param("targetId", "isolated-dummy-agent")
                        .param("reason", "Integration test activation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/killswitches"));
    }

    @Test
    @DisplayName("REST API endpoints for servers, tools, and agents work with Bearer token")
    void testRestEndpoints() throws Exception {
        // GET /api/v1/servers
        mockMvc.perform(get("/api/v1/servers")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // GET /api/v1/tools
        mockMvc.perform(get("/api/v1/tools")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // GET /api/v1/agents
        mockMvc.perform(get("/api/v1/agents")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Mock downstream endpoints respond successfully")
    void testMockDownstreamEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/mock/weather")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Tokyo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Tokyo"))
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(post("/api/v1/mock/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msg\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("MCP JSON-RPC discovery endpoints work as expected")
    void testMcpEndpoints() throws Exception {
        // initialize
        mockMvc.perform(post("/mcp")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"));

        // tools/list
        mockMvc.perform(post("/mcp")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"list-1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools").isArray());
    }

    @Test
    @DisplayName("Actuator health endpoint is open and reports UP")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
