package com.agentgate.killswitch.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kill_switch")
public class KillSwitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kill_switch_id")
    private Long killSwitchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 32, nullable = false)
    private KillSwitchScope scope;

    @Column(name = "target_id", length = 128)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private KillSwitchStatus status = KillSwitchStatus.ACTIVE;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "admin_id", length = 128, nullable = false)
    private String adminId;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt = Instant.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    public KillSwitch() {}

    public KillSwitch(KillSwitchScope scope, String targetId, String reason, String adminId) {
        this.scope = scope;
        this.targetId = targetId;
        this.reason = reason;
        this.adminId = adminId;
        this.status = KillSwitchStatus.ACTIVE;
        this.activatedAt = Instant.now();
    }

    public enum KillSwitchScope {
        GLOBAL_WRITES,
        AGENT,
        TOOL
    }

    public enum KillSwitchStatus {
        ACTIVE,
        INACTIVE
    }

    public Long getKillSwitchId() { return killSwitchId; }
    public void setKillSwitchId(Long killSwitchId) { this.killSwitchId = killSwitchId; }

    public KillSwitchScope getScope() { return scope; }
    public void setScope(KillSwitchScope scope) { this.scope = scope; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public KillSwitchStatus getStatus() { return status; }
    public void setStatus(KillSwitchStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
}
