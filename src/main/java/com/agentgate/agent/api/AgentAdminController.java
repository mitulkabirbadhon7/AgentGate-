package com.agentgate.agent.api;

import com.agentgate.agent.application.AgentDTOs;
import com.agentgate.agent.application.AgentService;
import com.agentgate.agent.domain.AgentClient;
import com.agentgate.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentAdminController {

    private final AgentService agentService;

    public AgentAdminController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgentDTOs.SecretRotationResponse>> registerAgent(
            @Valid @RequestBody AgentDTOs.CreateAgentRequest request) {
        AgentDTOs.SecretRotationResponse response = agentService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Agent successfully registered", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentDTOs.AgentResponse>>> listAgents() {
        return ResponseEntity.ok(ApiResponse.ok(agentService.listAgents()));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<AgentDTOs.AgentResponse>> getAgent(@PathVariable String clientId) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.getAgent(clientId)));
    }

    @PostMapping("/{clientId}/rotate-secret")
    public ResponseEntity<ApiResponse<AgentDTOs.SecretRotationResponse>> rotateSecret(@PathVariable String clientId) {
        return ResponseEntity.ok(ApiResponse.ok("Client secret rotated", agentService.rotateSecret(clientId)));
    }

    @PostMapping("/{clientId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable String clientId,
            @RequestParam AgentClient.AgentStatus status) {
        agentService.setStatus(clientId, status);
        return ResponseEntity.ok(ApiResponse.ok("Agent status updated to " + status, clientId));
    }
}
