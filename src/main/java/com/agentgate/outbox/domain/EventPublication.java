package com.agentgate.outbox.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_publication")
public class EventPublication {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "event_routing_key", length = 255, nullable = false)
    private String eventRoutingKey;

    @Column(name = "event_type", length = 128, nullable = false)
    private String eventType;

    @Column(name = "event_payload", columnDefinition = "TEXT", nullable = false)
    private String eventPayload;

    @Column(name = "listener_id", length = 128, nullable = false)
    private String listenerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EventPublication() {}

    public EventPublication(String eventRoutingKey, String eventType, String eventPayload, String listenerId) {
        this.id = UUID.randomUUID();
        this.eventRoutingKey = eventRoutingKey;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
        this.listenerId = listenerId;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED,
        RETRYING
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEventRoutingKey() { return eventRoutingKey; }
    public void setEventRoutingKey(String eventRoutingKey) { this.eventRoutingKey = eventRoutingKey; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventPayload() { return eventPayload; }
    public void setEventPayload(String eventPayload) { this.eventPayload = eventPayload; }

    public String getListenerId() { return listenerId; }
    public void setListenerId(String listenerId) { this.listenerId = listenerId; }

    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
