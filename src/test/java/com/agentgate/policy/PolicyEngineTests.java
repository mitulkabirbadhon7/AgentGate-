package com.agentgate.policy;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.policy.application.PolicyCacheService;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyEvaluator;
import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PolicyEngineTests {

    private PolicyEvaluator evaluator;
    private PolicyBindingRepository bindingRepository;
    private PolicyVersionRepository versionRepository;
    private PolicyCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bindingRepository = Mockito.mock(PolicyBindingRepository.class);
        versionRepository = Mockito.mock(PolicyVersionRepository.class);
        cacheService = new PolicyCacheService();
        evaluator = new PolicyEvaluator(bindingRepository, versionRepository, objectMapper);
    }

    @Test
    void whenNoBindingFound_evaluatesToDefaultDeny() {
        when(bindingRepository.findApplicableBindings("agent-01", 100L)).thenReturn(List.of());

        PolicyDTOs.EvaluationRequest req = new PolicyDTOs.EvaluationRequest(
                "agent-01", 100L, Map.of(), "user-01", "STANDARD", false
        );

        PolicyDTOs.EvaluationResult result = evaluator.evaluate(req);
        assertThat(result.decision()).isEqualTo(PolicyDTOs.Decision.DENY);
        assertThat(result.reason()).contains("Default deny");
    }

    @Test
    void whenRuleMatchesAmountCondition_returnsExpectedDecision() {
        Policy policy = new Policy("Refund Policy");
        policy.setPolicyId(1L);
        policy.setCurrentVersionId(10L);

        String rulesTree = """
                {
                  "rules": [
                    {
                      "id": "r-1",
                      "effect": "ALLOW",
                      "condition": { "field": "arguments.amount", "op": "LTE", "value": 500 },
                      "reason": "Small refund auto-allowed"
                    },
                    {
                      "id": "r-2",
                      "effect": "REQUIRE_APPROVAL",
                      "condition": { "field": "arguments.amount", "op": "GT", "value": 500 },
                      "reason": "Large refund requires review"
                    }
                  ]
                }
                """;

        PolicyVersion pv = new PolicyVersion(policy, 1, rulesTree, "admin", "hash1");
        pv.setVersionId(10L);

        PolicyBinding binding = new PolicyBinding();
        binding.setPolicy(policy);
        binding.setScope(PolicyBinding.BindingScope.GLOBAL);
        binding.setPriority(100);

        when(bindingRepository.findApplicableBindings("agent-01", 200L)).thenReturn(List.of(binding));
        when(versionRepository.findById(10L)).thenReturn(Optional.of(pv));

        // Test small refund -> ALLOW
        PolicyDTOs.EvaluationResult r1 = evaluator.evaluate(new PolicyDTOs.EvaluationRequest(
                "agent-01", 200L, Map.of("amount", 250), "user-01", "STANDARD", false
        ));
        assertThat(r1.decision()).isEqualTo(PolicyDTOs.Decision.ALLOW);

        // Test large refund -> REQUIRE_APPROVAL
        PolicyDTOs.EvaluationResult r2 = evaluator.evaluate(new PolicyDTOs.EvaluationRequest(
                "agent-01", 200L, Map.of("amount", 1200), "user-01", "STANDARD", false
        ));
        assertThat(r2.decision()).isEqualTo(PolicyDTOs.Decision.REQUIRE_APPROVAL);
    }

    @Test
    void cacheOutage_mediumOrHighRisk_failsClosed() {
        cacheService.recordStoreOutage();

        assertThatThrownBy(() -> cacheService.handleOutageEvaluation("agent:tool", Tool.RiskLevel.HIGH))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.POLICY_DENIED);
                });
    }

    @Test
    void cacheOutage_lowRiskWithFreshSnapshot_allowsWithin5Minutes() {
        PolicyDTOs.EvaluationResult cachedResult = new PolicyDTOs.EvaluationResult(
                PolicyDTOs.Decision.ALLOW, "Cached ok", 1L, 10L, false, 5L
        );
        cacheService.putSnapshot("agent-01:300", cachedResult);
        cacheService.recordStoreOutage();

        PolicyDTOs.EvaluationResult result = cacheService.handleOutageEvaluation("agent-01:300", Tool.RiskLevel.LOW);
        assertThat(result).isNotNull();
        assertThat(result.decision()).isEqualTo(PolicyDTOs.Decision.ALLOW);
    }
}
