package com.agentgate.agent.application;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class AgentService {

    private final AgentClientRepository agentClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AgentService(AgentClientRepository agentClientRepository, PasswordEncoder passwordEncoder) {
        this.agentClientRepository = agentClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AgentDTOs.SecretRotationResponse registerAgent(AgentDTOs.CreateAgentRequest request) {
        if (agentClientRepository.existsById(request.clientId())) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Agent with clientId '" + request.clientId() + "' already exists");
        }
        if (agentClientRepository.existsByName(request.name())) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Agent with name '" + request.name() + "' already exists");
        }

        String rawSecret = generateSecureSecret();
        String secretHash = passwordEncoder.encode(rawSecret);

        AgentClient agent = new AgentClient(
                request.clientId(),
                request.name(),
                secretHash,
                request.displayName()
        );
        agentClientRepository.save(agent);

        return new AgentDTOs.SecretRotationResponse(
                agent.getClientId(),
                rawSecret,
                "Agent successfully registered. Store this client secret safely; it cannot be retrieved again."
        );
    }

    @Transactional
    public AgentDTOs.SecretRotationResponse rotateSecret(String clientId) {
        AgentClient agent = agentClientRepository.findById(clientId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Agent not found: " + clientId));

        String rawSecret = generateSecureSecret();
        agent.setClientSecretHash(passwordEncoder.encode(rawSecret));
        agentClientRepository.save(agent);

        return new AgentDTOs.SecretRotationResponse(
                agent.getClientId(),
                rawSecret,
                "Client secret rotated successfully. Previous credentials have been invalidated."
        );
    }

    @Transactional(readOnly = true)
    public AgentDTOs.AgentResponse getAgent(String clientId) {
        AgentClient agent = agentClientRepository.findById(clientId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Agent not found: " + clientId));
        return AgentDTOs.AgentResponse.from(agent);
    }

    @Transactional(readOnly = true)
    public List<AgentDTOs.AgentResponse> listAgents() {
        return agentClientRepository.findAll().stream()
                .map(AgentDTOs.AgentResponse::from)
                .toList();
    }

    @Transactional
    public void setStatus(String clientId, AgentClient.AgentStatus status) {
        AgentClient agent = agentClientRepository.findById(clientId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Agent not found: " + clientId));
        agent.setStatus(status);
        agentClientRepository.save(agent);
    }

    private String generateSecureSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
