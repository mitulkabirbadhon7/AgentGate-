package com.agentgate.tool.application;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
public class ToolRegistryService {

    private final DownstreamServerRepository serverRepository;
    private final ToolRepository toolRepository;
    private final ObjectMapper objectMapper;

    public ToolRegistryService(
            DownstreamServerRepository serverRepository,
            ToolRepository toolRepository,
            ObjectMapper objectMapper) {
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ToolDTOs.ServerResponse registerServer(ToolDTOs.CreateServerRequest request) {
        if (serverRepository.existsByName(request.name())) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Downstream server with name '" + request.name() + "' already exists");
        }

        validateEndpointUri(request.endpointUrl());

        DownstreamServer server = new DownstreamServer(
                request.name(),
                request.type(),
                request.endpointUrl(),
                request.healthCheckIntervalSec() > 0 ? request.healthCheckIntervalSec() : 30,
                request.circuitOpenThreshold() > 0 ? request.circuitOpenThreshold() : 5
        );
        server = serverRepository.save(server);
        return ToolDTOs.ServerResponse.from(server);
    }

    @Transactional(readOnly = true)
    public List<ToolDTOs.ServerResponse> listServers() {
        return serverRepository.findAll().stream()
                .map(ToolDTOs.ServerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ToolDTOs.ServerResponse getServer(Long serverId) {
        DownstreamServer server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Server not found: " + serverId));
        return ToolDTOs.ServerResponse.from(server);
    }

    @Transactional
    public void deleteServer(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Server not found: " + serverId);
        }
        serverRepository.deleteById(serverId);
    }

    @Transactional
    public ToolDTOs.ToolResponse registerTool(ToolDTOs.CreateToolRequest request) {
        if (toolRepository.existsByName(request.name())) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Tool with name '" + request.name() + "' already exists");
        }

        DownstreamServer server = serverRepository.findById(request.serverId())
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Downstream server not found: " + request.serverId()));

        // Validate JSON schema format
        validateJsonSchemaSyntax(request.inputSchema());

        Tool tool = new Tool(
                server,
                request.name(),
                request.description(),
                request.inputSchema(),
                request.riskLevel() != null ? request.riskLevel() : Tool.RiskLevel.LOW,
                request.routingHint()
        );
        tool = toolRepository.save(tool);
        return ToolDTOs.ToolResponse.from(tool);
    }

    @Transactional(readOnly = true)
    public List<ToolDTOs.ToolResponse> listTools() {
        return toolRepository.findAll().stream()
                .map(ToolDTOs.ToolResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ToolDTOs.ToolResponse> listActiveTools() {
        return toolRepository.findAllActiveTools().stream()
                .map(ToolDTOs.ToolResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ToolDTOs.ToolResponse getTool(Long toolId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found: " + toolId));
        return ToolDTOs.ToolResponse.from(tool);
    }

    @Transactional(readOnly = true)
    public ToolDTOs.ToolResponse getToolByName(String name) {
        Tool tool = toolRepository.findByName(name)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found with name: " + name));
        return ToolDTOs.ToolResponse.from(tool);
    }

    @Transactional
    public void setToolStatus(Long toolId, Tool.ToolStatus status) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found: " + toolId));
        tool.setStatus(status);
        toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long toolId) {
        if (!toolRepository.existsById(toolId)) {
            throw new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found: " + toolId);
        }
        toolRepository.deleteById(toolId);
    }

    private void validateEndpointUri(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Endpoint URL must have http or https scheme");
            }
        } catch (IllegalArgumentException e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Invalid endpoint URI format: " + endpoint);
        }
    }

    private void validateJsonSchemaSyntax(String schemaStr) {
        try {
            objectMapper.readTree(schemaStr);
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, "Invalid JSON Schema format: " + e.getMessage());
        }
    }
}
