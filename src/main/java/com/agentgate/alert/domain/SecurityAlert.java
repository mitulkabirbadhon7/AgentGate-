package com.agentgate.alert.domain;

import com.agentgate.agent.domain.AgentClient;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "security_alert")
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "alert_type", length = 64, nullable = false)
    private String alertType;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "triggered_by")
    private AgentClient triggeredBy;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "details", columnDefinition = "TEXT", nullable = false)
    private String details;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SecurityAlert() {}

    public SecurityAlert(String alertType, String severity, AgentClient triggeredBy, String eventId, String details) {
        this.alertType = alertType;
        this.severity = severity;
        this.triggeredBy = triggeredBy;
        this.eventId = eventId;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public AgentClient getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(AgentClient triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
