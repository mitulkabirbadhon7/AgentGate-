package com.agentgate.agent.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "end_user", uniqueConstraints = {
    @UniqueConstraint(name = "uk_end_user_issuer_ref", columnNames = {"issuer", "external_ref"})
})
public class EndUser {

    @Id
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "external_ref", length = 128, nullable = false)
    private String externalRef;

    @Column(name = "issuer", length = 255, nullable = false)
    private String issuer;

    @Column(name = "clearance_level", length = 32, nullable = false)
    private String clearanceLevel = "STANDARD";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EndUser() {}

    public EndUser(String userId, String externalRef, String issuer, String clearanceLevel) {
        this.userId = userId;
        this.externalRef = externalRef;
        this.issuer = issuer;
        this.clearanceLevel = clearanceLevel != null ? clearanceLevel : "STANDARD";
        this.createdAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
