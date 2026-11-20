package com.agentgate.resilience;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.alert.infrastructure.SecurityAlertRepository;
import com.agentgate.quota.application.RateLimitDTOs;
import com.agentgate.quota.application.RateLimitService;
import com.agentgate.quota.domain.RateLimitPolicy;
import com.agentgate.quota.infrastructure.QuotaPolicyRepository;
import com.agentgate.quota.infrastructure.RateLimitPolicyRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RateLimitAndQuotaTests {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RateLimitPolicyRepository rateLimitRepository;

    @Autowired
    private QuotaPolicyRepository quotaRepository;

    @Autowired
    private SecurityAlertRepository alertRepository;

    @Autowired
    private AgentClientRepository agentRepository;

    @Autowired
    private DownstreamServerRepository serverRepository;

    @Autowired
    private ToolRepository toolRepository;

    private AgentClient agent;
    private Tool lowRiskTool;
    private Tool highRiskTool;

    @BeforeEach
    void setUp() {
        rateLimitRepository.deleteAll();
        quotaRepository.deleteAll();
        alertRepository.deleteAll();
        rateLimitService.resetCountersForTest();
        rateLimitService.setSimulateRedisOutage(false);

        agent = agentRepository.findById("quota-agent-01")
                .orElseGet(() -> agentRepository.save(new AgentClient("quota-agent-01", "Quota Agent", "hash", "Quota Agent")));

        DownstreamServer server = serverRepository.findByName("quota-server-01")
                .orElseGet(() -> serverRepository.save(new DownstreamServer("quota-server-01", DownstreamServer.ServerType.REST, "http://localhost:8080", 30, 5)));

        lowRiskTool = toolRepository.findByName("quota_low_tool")
                .orElseGet(() -> toolRepository.save(new Tool(server, "quota_low_tool", "Low Risk Tool", "{\"type\":\"object\"}", RiskLevel.LOW, "GET /low")));

        highRiskTool = toolRepository.findByName("quota_high_tool")
                .orElseGet(() -> toolRepository.save(new Tool(server, "quota_high_tool", "High Risk Tool", "{\"type\":\"object\"}", RiskLevel.HIGH, "POST /high")));
    }

    @Test
    @DisplayName("Rate limit enforcement: permits requests up to max, throws RATE_LIMIT_EXCEEDED and logs alert on breach")
    void testRateLimitEnforcement() {
        rateLimitService.createRateLimit(new RateLimitDTOs.CreateRateLimitRequest(
                RateLimitPolicy.PolicyScope.AGENT_TOOL,
                agent.getClientId(),
                lowRiskTool.getToolId(),
                60,
                3
        ));

        // 3 requests should pass
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());

        // 4th request must fail
        assertThatThrownBy(() -> rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of()))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
                });

        // Verify security alert was generated
        List<SecurityAlert> alerts = alertRepository.findAll();
        assertThat(alerts).isNotEmpty();
        SecurityAlert alert = alerts.getFirst();
        assertThat(alert.getAlertType()).isEqualTo("RATE_EXCEEDED");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        assertThat(alert.getTriggeredBy().getClientId()).isEqualTo(agent.getClientId());
    }

    @Test
    @DisplayName("Daily UTC quota budget: throws QUOTA_EXCEEDED and logs CRITICAL alert when daily operations exceed cap")
    void testDailyQuotaEnforcement() {
        rateLimitService.createQuota(new RateLimitDTOs.CreateQuotaRequest(
                RateLimitPolicy.PolicyScope.AGENT_TOOL,
                agent.getClientId(),
                lowRiskTool.getToolId(),
                2L,
                "DAY"
        ));

        // 2 requests allowed
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());

        // 3rd request violates daily UTC quota
        assertThatThrownBy(() -> rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of()))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.QUOTA_EXCEEDED);
                });

        // Verify critical alert
        List<SecurityAlert> alerts = alertRepository.findAll();
        assertThat(alerts).anyMatch(a -> a.getAlertType().equals("QUOTA_EXCEEDED") && a.getSeverity().equals("CRITICAL"));
    }

    @Test
    @DisplayName("Redis outage fail-closed: High risk tool fails closed with SERVICE_UNAVAILABLE")
    void testRedisOutageFailClosedForHighRisk() {
        rateLimitService.setSimulateRedisOutage(true);

        assertThatThrownBy(() -> rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), highRiskTool, Map.of()))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    @DisplayName("Redis outage resilience: Low risk tool falls back to bounded in-memory counter")
    void testRedisOutageFallbackForLowRisk() {
        rateLimitService.setSimulateRedisOutage(true);

        // Low risk tool invocation should not throw POLICY_STORE_UNREACHABLE
        rateLimitService.enforceLimitsAndQuotas(agent.getClientId(), lowRiskTool, Map.of());
    }
}
