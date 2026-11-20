package com.agentgate.policy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "policy")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "name", length = 128, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "active_version_locks", nullable = false)
    private boolean activeVersionLocks = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Policy() {}

    public Policy(String name) {
        this.name = name;
        this.status = PolicyStatus.ACTIVE;
        this.activeVersionLocks = true;
        this.createdAt = Instant.now();
    }

    public enum PolicyStatus {
        ACTIVE,
        DRAFT,
        ARCHIVED
    }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }

    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }

    public boolean isActiveVersionLocks() { return activeVersionLocks; }
    public void setActiveVersionLocks(boolean activeVersionLocks) { this.activeVersionLocks = activeVersionLocks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
