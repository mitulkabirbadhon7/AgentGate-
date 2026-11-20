package com.agentgate.policy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "policy_version", uniqueConstraints = {
    @UniqueConstraint(name = "uk_policy_version", columnNames = {"policy_id", "version_number"})
})
public class PolicyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "rules_tree", columnDefinition = "TEXT", nullable = false)
    private String rulesTree;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    @Column(name = "audit_by_user", length = 128, nullable = false)
    private String auditByUser;

    @Column(name = "version_hash", length = 64, nullable = false)
    private String versionHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public PolicyVersion() {}

    public PolicyVersion(Policy policy, int versionNumber, String rulesTree, String auditByUser, String versionHash) {
        this.policy = policy;
        this.versionNumber = versionNumber;
        this.rulesTree = rulesTree;
        this.auditByUser = auditByUser;
        this.versionHash = versionHash;
        this.effectiveFrom = Instant.now();
        this.createdAt = Instant.now();
    }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public Policy getPolicy() { return policy; }
    public void setPolicy(Policy policy) { this.policy = policy; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getRulesTree() { return rulesTree; }
    public void setRulesTree(String rulesTree) { this.rulesTree = rulesTree; }

    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public String getAuditByUser() { return auditByUser; }
    public void setAuditByUser(String auditByUser) { this.auditByUser = auditByUser; }

    public String getVersionHash() { return versionHash; }
    public void setVersionHash(String versionHash) { this.versionHash = versionHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
