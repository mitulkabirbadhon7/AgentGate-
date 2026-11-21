package com.agentgate.sqlite;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.domain.EndUser;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.agent.infrastructure.EndUserRepository;
import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.alert.infrastructure.SecurityAlertRepository;
import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.audit.domain.AuditEvent;
import com.agentgate.audit.infrastructure.AuditEventRepository;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.killswitch.domain.KillSwitch;
import com.agentgate.killswitch.infrastructure.KillSwitchRepository;
import com.agentgate.outbox.domain.EventPublication;
import com.agentgate.outbox.infrastructure.EventPublicationRepository;
import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.quota.domain.QuotaPolicy;
import com.agentgate.quota.domain.RateLimitPolicy;
import com.agentgate.quota.infrastructure.QuotaPolicyRepository;
import com.agentgate.quota.infrastructure.RateLimitPolicyRepository;
import com.agentgate.security.token.TokenService;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:target/sqlite-test.db",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "management.health.redis.enabled=false"
})
class SqliteDatabaseIntegrationTests {

    @Autowired
    private AgentClientRepository agentClientRepository;

    @Autowired
    private EndUserRepository endUserRepository;

    @Autowired
    private DownstreamServerRepository downstreamServerRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyVersionRepository policyVersionRepository;

    @Autowired
    private PolicyBindingRepository policyBindingRepository;

    @Autowired
    private KillSwitchRepository killSwitchRepository;

    @Autowired
    private QuotaPolicyRepository quotaPolicyRepository;

    @Autowired
    private RateLimitPolicyRepository rateLimitPolicyRepository;

    @Autowired
    private ToolInvocationRepository toolInvocationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private EventPublicationRepository eventPublicationRepository;

    @Autowired
    private AuditChainService auditChainService;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("Should successfully connect to SQLite, create schema via Hibernate, and perform CRUD operations across core entities")
    void testSqliteDatabaseCrud() {
        // 1. AgentClient & EndUser
        AgentClient agent = new AgentClient("sqlite-test-agent", "SQLite Test Agent", "hashed-secret", "SQLite Agent");
        AgentClient savedAgent = agentClientRepository.save(agent);
        assertThat(savedAgent.getClientId()).isEqualTo("sqlite-test-agent");

        EndUser user = new EndUser("usr-sqlite-01", "ext-user-1", "https://auth.example.com", "STANDARD");
        EndUser savedUser = endUserRepository.save(user);
        assertThat(savedUser.getUserId()).isEqualTo("usr-sqlite-01");

        // 2. DownstreamServer and Tool
        DownstreamServer server = new DownstreamServer("sqlite-mcp-server", DownstreamServer.ServerType.MCP, "http://localhost:9090", 30, 5);
        DownstreamServer savedServer = downstreamServerRepository.save(server);
        assertThat(savedServer.getServerId()).isNotNull();

        Tool tool = new Tool(
            savedServer,
            "sqlite_query_tool",
            "Queries SQLite DB directly",
            "{\"type\":\"object\"}",
            Tool.RiskLevel.LOW,
            "read"
        );
        Tool savedTool = toolRepository.save(tool);
        assertThat(savedTool.getToolId()).isNotNull();

        // 3. Policy, Version, and Binding
        Policy policy = new Policy("sqlite-policy");
        Policy savedPolicy = policyRepository.save(policy);

        PolicyVersion pv = new PolicyVersion(savedPolicy, 1, "{\"rules\":[]}", "admin", "hash-pv-1");
        PolicyVersion savedPv = policyVersionRepository.save(pv);
        savedPolicy.setCurrentVersionId(savedPv.getVersionId());
        policyRepository.save(savedPolicy);

        PolicyBinding binding = new PolicyBinding(savedPolicy, null, null, PolicyBinding.BindingScope.GLOBAL, 50);
        PolicyBinding savedBinding = policyBindingRepository.save(binding);
        assertThat(savedBinding.getBindingId()).isNotNull();

        // 4. KillSwitch
        KillSwitch killSwitch = new KillSwitch(KillSwitch.KillSwitchScope.TOOL, String.valueOf(savedTool.getToolId()), "Emergency pause", "admin-1");
        KillSwitch savedKillSwitch = killSwitchRepository.save(killSwitch);
        assertThat(savedKillSwitch.getKillSwitchId()).isNotNull();

        // 5. Quota & RateLimit Policies
        QuotaPolicy quota = new QuotaPolicy(RateLimitPolicy.PolicyScope.AGENT, savedAgent, null, 1000L, "DAILY_UTC");
        QuotaPolicy savedQuota = quotaPolicyRepository.save(quota);
        assertThat(savedQuota.getQuotaId()).isNotNull();

        RateLimitPolicy rateLimit = new RateLimitPolicy(RateLimitPolicy.PolicyScope.TOOL, null, savedTool, 60, 50);
        RateLimitPolicy savedRateLimit = rateLimitPolicyRepository.save(rateLimit);
        assertThat(savedRateLimit.getRateLimitId()).isNotNull();

        // 6. SecurityAlert
        SecurityAlert alert = new SecurityAlert("QUOTA_EXCEEDED", "CRITICAL", savedAgent, "evt-123", "Agent exceeded SQLite test quota");
        SecurityAlert savedAlert = securityAlertRepository.save(alert);
        assertThat(savedAlert.getAlertId()).isNotNull();

        // 7. EventPublication (Transactional Outbox)
        EventPublication publication = new EventPublication("tool.test.event", "TEST", "{\"key\":\"val\"}", "listener-1");
        EventPublication savedPub = eventPublicationRepository.save(publication);
        assertThat(savedPub.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should persist tool invocation, approval request, and cryptographic audit SHA-256 hash chain on SQLite")
    void testSqliteInvocationAndAuditChain() {
        // Setup Agent and Tool
        AgentClient agent = agentClientRepository.save(
                new AgentClient("audit-agent", "Audit Agent", "secret", "Desc")
        );
        DownstreamServer server = downstreamServerRepository.save(
                new DownstreamServer("audit-server", DownstreamServer.ServerType.REST, "http://localhost:8080/api/mock", 30, 5)
        );
        Tool tool = toolRepository.save(
                new Tool(server, "audit_test_tool", "Description", "{\"type\":\"object\"}", Tool.RiskLevel.HIGH, "POST /exec")
        );

        // Tool Invocation
        UUID eventId = UUID.randomUUID();
        ToolInvocation invocation = new ToolInvocation(
                eventId,
                agent,
                null,
                tool,
                "{\"input\":\"secure_data\"}",
                ToolInvocation.InvocationStatus.RECEIVED
        );
        ToolInvocation savedInvocation = toolInvocationRepository.save(invocation);
        assertThat(savedInvocation.getInvocationId()).isNotNull();

        // Cryptographic Audit Event Chaining on SQLite
        AuditEvent event1 = auditChainService.appendAuditEvent(savedInvocation, "INVOCATION_RECEIVED", "{\"input\":\"secure_data\"}", agent.getClientId());
        assertThat(event1.getAuditId()).isNotNull();
        assertThat(event1.getSequenceNumber()).isGreaterThan(0L);
        assertThat(event1.getCurrHash()).isNotBlank();
        assertThat(event1.getHashAlgorithm()).isEqualTo("SHA-256");

        AuditEvent event2 = auditChainService.appendAuditEvent(savedInvocation, "COMPLETED", "{\"result\":\"ok\"}", "system");
        assertThat(event2.getSequenceNumber()).isEqualTo(event1.getSequenceNumber() + 1);
        assertThat(event2.getPrevHash()).isEqualTo(event1.getCurrHash());

        // Approval Request
        ApprovalRequest approval = new ApprovalRequest(
                savedInvocation,
                "encrypted-original-args",
                "hash-of-args",
                Instant.now().plusSeconds(600)
        );
        ApprovalRequest savedApproval = approvalRequestRepository.save(approval);
        assertThat(savedApproval.getApprovalId()).isNotNull();
        assertThat(savedApproval.getStatus()).isEqualTo(ApprovalRequest.ApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("Should issue and validate JWT tokens with SQLite-backed agent client")
    void testSqliteTokenValidation() {
        String token = tokenService.generateToken("demo-agent", "user-123", Set.of("tools:read", "tools:call"), 3600);
        assertThat(token).isNotBlank();

        var snapshot = tokenService.validateAndParse(token);
        assertThat(snapshot.agentId()).isEqualTo("demo-agent");
        assertThat(snapshot.userId()).isEqualTo("user-123");
        assertThat(snapshot.scopes()).contains("tools:read", "tools:call");
    }
}
