package com.agentgate.alert.application;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.alert.infrastructure.SecurityAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SecurityAlertService {

    private final SecurityAlertRepository alertRepository;
    private final AgentClientRepository agentRepository;

    public SecurityAlertService(SecurityAlertRepository alertRepository, AgentClientRepository agentRepository) {
        this.alertRepository = alertRepository;
        this.agentRepository = agentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurityAlert(String type, String severity, String agentId, String details) {
        AgentClient agent = agentId != null ? agentRepository.findById(agentId).orElse(null) : null;
        SecurityAlert alert = new SecurityAlert(type, severity, agent, null, details);
        alertRepository.saveAndFlush(alert);
    }

    @Transactional(readOnly = true)
    public List<SecurityAlert> listRecentAlerts() {
        return alertRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
