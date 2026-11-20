package com.agentgate.policy.application;

import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class PolicyEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);
    private final PolicyBindingRepository bindingRepository;
    private final PolicyVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public PolicyEvaluator(
            PolicyBindingRepository bindingRepository,
            PolicyVersionRepository versionRepository,
            ObjectMapper objectMapper) {
        this.bindingRepository = bindingRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    public PolicyDTOs.EvaluationResult evaluate(PolicyDTOs.EvaluationRequest request) {
        long startTime = System.currentTimeMillis();

        List<PolicyBinding> bindings = bindingRepository.findApplicableBindings(request.agentId(), request.toolId());

        if (bindings.isEmpty()) {
            long latency = System.currentTimeMillis() - startTime;
            return new PolicyDTOs.EvaluationResult(
                    PolicyDTOs.Decision.DENY,
                    "Default deny: no policy bindings found for agent '" + request.agentId() + "' and tool '" + request.toolId() + "'",
                    null,
                    null,
                    request.dryRun(),
                    latency
            );
        }

        for (PolicyBinding binding : bindings) {
            Long activeVersionId = binding.getPolicy().getCurrentVersionId();
            if (activeVersionId == null) {
                continue;
            }

            PolicyVersion version = versionRepository.findById(activeVersionId).orElse(null);
            if (version == null) {
                continue;
            }

            PolicyDTOs.Decision decision = evaluateVersionRules(version, request);
            if (decision != null) {
                long latency = System.currentTimeMillis() - startTime;
                return new PolicyDTOs.EvaluationResult(
                        decision,
                        "Matched policy '" + binding.getPolicy().getName() + "' (v" + version.getVersionNumber() + ") via " + binding.getScope(),
                        binding.getPolicy().getPolicyId(),
                        version.getVersionId(),
                        request.dryRun(),
                        latency
                );
            }
        }

        long latency = System.currentTimeMillis() - startTime;
        return new PolicyDTOs.EvaluationResult(
                PolicyDTOs.Decision.DENY,
                "Default deny: no rule in applicable policies matched with an ALLOW decision",
                null,
                null,
                request.dryRun(),
                latency
        );
    }

    private PolicyDTOs.Decision evaluateVersionRules(PolicyVersion version, PolicyDTOs.EvaluationRequest request) {
        try {
            JsonNode root = objectMapper.readTree(version.getRulesTree());
            JsonNode rulesNode = root.get("rules");
            if (rulesNode == null || !rulesNode.isArray()) {
                return null;
            }

            for (JsonNode rule : rulesNode) {
                String effectStr = rule.has("effect") ? rule.get("effect").asText().toUpperCase() : "DENY";
                JsonNode condition = rule.get("condition");

                boolean conditionMatched = true;
                if (condition != null && !condition.isNull() && condition.isObject()) {
                    conditionMatched = evaluateCondition(condition, request);
                }

                if (conditionMatched) {
                    return switch (effectStr) {
                        case "ALLOW" -> PolicyDTOs.Decision.ALLOW;
                        case "REQUIRE_APPROVAL" -> PolicyDTOs.Decision.REQUIRE_APPROVAL;
                        case "DENY" -> PolicyDTOs.Decision.DENY;
                        default -> PolicyDTOs.Decision.DENY;
                    };
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse or evaluate rules tree for version ID: {}", version.getVersionId(), e);
            throw new AgentGateException(ErrorCode.INTERNAL_ERROR, "Error evaluating policy rules AST: " + e.getMessage());
        }
        return null;
    }

    private boolean evaluateCondition(JsonNode condition, PolicyDTOs.EvaluationRequest request) {
        String field = condition.has("field") ? condition.get("field").asText() : "";
        String op = condition.has("op") ? condition.get("op").asText().toUpperCase() : "EQ";
        JsonNode expectedValue = condition.get("value");

        Object actualVal = resolveFieldValue(field, request);
        if (actualVal == null) {
            return false;
        }

        return switch (op) {
            case "EQ" -> compareEquals(actualVal, expectedValue);
            case "NEQ" -> !compareEquals(actualVal, expectedValue);
            case "LT" -> compareNumeric(actualVal, expectedValue) < 0;
            case "LTE" -> compareNumeric(actualVal, expectedValue) <= 0;
            case "GT" -> compareNumeric(actualVal, expectedValue) > 0;
            case "GTE" -> compareNumeric(actualVal, expectedValue) >= 0;
            default -> false;
        };
    }

    private Object resolveFieldValue(String field, PolicyDTOs.EvaluationRequest request) {
        if (field.startsWith("arguments.")) {
            String argKey = field.substring("arguments.".length());
            return (request.arguments() != null) ? request.arguments().get(argKey) : null;
        } else if ("user_id".equals(field)) {
            return request.userId();
        } else if ("clearance_level".equals(field)) {
            return request.clearanceLevel();
        } else if ("agent_id".equals(field)) {
            return request.agentId();
        }
        return null;
    }

    private boolean compareEquals(Object actual, JsonNode expected) {
        if (expected == null || expected.isNull()) return actual == null;
        if (actual instanceof Number actualNum && expected.isNumber()) {
            return new BigDecimal(actualNum.toString()).compareTo(new BigDecimal(expected.asText())) == 0;
        }
        return String.valueOf(actual).equals(expected.asText());
    }

    private int compareNumeric(Object actual, JsonNode expected) {
        if (!(actual instanceof Number actualNum) || !expected.isNumber()) {
            return -1;
        }
        BigDecimal a = new BigDecimal(actualNum.toString());
        BigDecimal b = new BigDecimal(expected.asText());
        return a.compareTo(b);
    }
}
