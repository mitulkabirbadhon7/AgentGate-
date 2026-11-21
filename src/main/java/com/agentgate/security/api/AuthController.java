package com.agentgate.security.api;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.security.token.TokenService;
import com.agentgate.shared.api.ApiResponse;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AgentClientRepository agentRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AgentClientRepository agentRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public record TokenRequest(String clientId, String clientSecret) {}

    public record TokenResponse(
            String access_token,
            String token_type,
            long expires_in,
            String scope
    ) {}

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> issueToken(
            @RequestBody(required = false) TokenRequest request,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String clientSecret) {

        String cid = (request != null && request.clientId() != null) ? request.clientId() : clientId;
        String secret = (request != null && request.clientSecret() != null) ? request.clientSecret() : clientSecret;

        if (cid == null || cid.isBlank()) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Missing 'clientId' in authentication request");
        }

        AgentClient agent = agentRepository.findById(cid)
                .orElseThrow(() -> new AgentGateException(ErrorCode.UNAUTHORIZED, "Agent client '" + cid + "' not found"));

        if (agent.getStatus() != AgentClient.AgentStatus.ACTIVE) {
            throw new AgentGateException(ErrorCode.FORBIDDEN, "Agent client status is not ACTIVE: " + agent.getStatus());
        }

        if (secret != null && !secret.isBlank()) {
            // Verify secret hash if secret is provided
            boolean matches = passwordEncoder.matches(secret, agent.getClientSecretHash());
            if (!matches && !secret.equals(agent.getClientSecretHash())) {
                throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Invalid client credentials");
            }
        }

        long ttlSeconds = 3600;
        Set<String> scopes = Set.of("tools:read", "tools:call", "admin:all");
        String jwt = tokenService.generateToken(cid, null, scopes, ttlSeconds);

        TokenResponse tokenResponse = new TokenResponse(jwt, "Bearer", ttlSeconds, String.join(" ", scopes));
        return ResponseEntity.ok(ApiResponse.ok("Token generated successfully", tokenResponse));
    }

    @GetMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> issueTokenGet(
            @RequestParam(defaultValue = "demo-agent") String clientId) {
        return issueToken(null, clientId, null);
    }
}
