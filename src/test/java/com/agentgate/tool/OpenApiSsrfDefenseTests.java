package com.agentgate.tool;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.application.OpenApiImportService;
import com.agentgate.tool.application.ToolDTOs;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OpenApiSsrfDefenseTests {

    private OpenApiImportService openApiImportService;
    private DownstreamServerRepository serverRepository;
    private ToolRepository toolRepository;

    @BeforeEach
    void setUp() {
        serverRepository = Mockito.mock(DownstreamServerRepository.class);
        toolRepository = Mockito.mock(ToolRepository.class);
        openApiImportService = new OpenApiImportService(
                serverRepository,
                toolRepository,
                new ObjectMapper()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/spec.json",
            "http://localhost:8080/openapi.json",
            "http://169.254.169.254/latest/meta-data/",
            "http://metadata.google.internal/computeMetadata/v1/",
            "file:///etc/passwd",
            "gopher://evil.com"
    })
    void validateSsrf_blocksDangerousDestinations(String dangerousUrl) {
        assertThatThrownBy(() -> openApiImportService.validateSsrfSafeUrl(dangerousUrl))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isIn(ErrorCode.FORBIDDEN, ErrorCode.VALIDATION_FAILED);
                });
    }

    @Test
    void importOpenApi_fromValidRawJson_registersToolsSuccessfully() {
        String rawOpenApi = """
                {
                  "openapi": "3.0.0",
                  "info": { "title": "Customer Service", "version": "1.0" },
                  "servers": [{ "url": "https://api.external.com" }],
                  "paths": {
                    "/customers/{id}": {
                      "get": {
                        "operationId": "getCustomerById",
                        "summary": "Retrieve customer by ID",
                        "parameters": [
                          { "name": "id", "in": "path", "required": true, "schema": { "type": "string" } }
                        ]
                      }
                    }
                  }
                }
                """;

        when(serverRepository.findByName("customer-service")).thenReturn(Optional.empty());
        when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(toolRepository.existsByName(any())).thenReturn(false);
        when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ToolDTOs.OpenApiImportRequest req = new ToolDTOs.OpenApiImportRequest(
                null,
                rawOpenApi,
                "customer-service",
                Tool.RiskLevel.LOW
        );

        ToolDTOs.OpenApiImportSummary summary = openApiImportService.importOpenApi(req);

        assertThat(summary).isNotNull();
        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.toolNames()).contains("getCustomerById");
    }
}
