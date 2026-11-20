package com.agentgate.policy;

import com.agentgate.invocation.guard.SchemaValidationGuard;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaValidationGuardTests {

    private SchemaValidationGuard guard;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        guard = new SchemaValidationGuard(objectMapper);
    }

    @Test
    void validArguments_matchSchemaSuccessfully() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "account_id": { "type": "string" },
                    "amount": { "type": "number" }
                  },
                  "required": ["account_id", "amount"]
                }
                """;
        Tool tool = new Tool(null, "transfer_funds", "Transfer funds", schema, Tool.RiskLevel.HIGH, "POST /transfer");

        Map<String, Object> args = Map.of(
                "account_id", "acc-1001",
                "amount", 250.50
        );

        JsonNode result = guard.validateAndNormalize(tool, args);
        assertThat(result).isNotNull();
        assertThat(result.get("account_id").asText()).isEqualTo("acc-1001");
        assertThat(result.get("amount").asDouble()).isEqualTo(250.50);
    }

    @Test
    void missingRequiredField_throwsValidationException() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "account_id": { "type": "string" }
                  },
                  "required": ["account_id"]
                }
                """;
        Tool tool = new Tool(null, "lookup_account", "Lookup account", schema, Tool.RiskLevel.LOW, "GET /account");

        Map<String, Object> args = Map.of("other_field", "value");

        assertThatThrownBy(() -> guard.validateAndNormalize(tool, args))
                .isInstanceOf(AgentGateException.class)
                .hasMessageContaining("validation failed");
    }
}
