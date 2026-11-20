package com.agentgate.invocation.guard;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SchemaValidationGuard {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<String, JsonSchema> compiledSchemaCache = new ConcurrentHashMap<>();

    public SchemaValidationGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    public JsonNode validateAndNormalize(Tool tool, Map<String, Object> arguments) {
        if (tool == null) {
            throw new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Target tool definition not found");
        }

        String rawSchema = tool.getInputSchema();
        if (rawSchema == null || rawSchema.isBlank()) {
            return objectMapper.valueToTree(arguments != null ? arguments : Collections.emptyMap());
        }

        JsonSchema schema = compiledSchemaCache.computeIfAbsent(tool.getName() + ":" + rawSchema.hashCode(), k -> {
            try {
                return schemaFactory.getSchema(rawSchema);
            } catch (Exception e) {
                throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Failed to compile tool JSON Schema: " + e.getMessage());
            }
        });

        JsonNode argsNode = objectMapper.valueToTree(arguments != null ? arguments : Collections.emptyMap());
        Set<ValidationMessage> errors = schema.validate(argsNode);

        if (!errors.isEmpty()) {
            String errorMsg = errors.iterator().next().getMessage();
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Tool argument validation failed: " + errorMsg);
        }

        return argsNode;
    }
}
