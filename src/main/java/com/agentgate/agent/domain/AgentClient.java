package com.agentgate.agent.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agent_client")
public class AgentClient {

    @Id
    @Column(name = "client_id", length = 64, nullable = false)
    private String clientId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "client_secret_hash", length = 255, nullable = false)
    private String clientSecretHash;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private AgentStatus status = AgentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AgentClient() {}

    public AgentClient(String clientId, String name, String clientSecretHash, String displayName) {
        this.clientId = clientId;
        this.name = name;
        this.clientSecretHash = clientSecretHash;
        this.displayName = displayName;
        this.status = AgentStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public enum AgentStatus {
        ACTIVE,
        SUSPENDED,
        REVOKED
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClientSecretHash() { return clientSecretHash; }
    public void setClientSecretHash(String clientSecretHash) { this.clientSecretHash = clientSecretHash; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() {
        return displayName != null && !displayName.isBlank() ? displayName : name;
    }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
