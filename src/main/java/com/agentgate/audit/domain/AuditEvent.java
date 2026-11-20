package com.agentgate.audit.domain;

import com.agentgate.invocation.domain.ToolInvocation;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invocation_id", nullable = false)
    private ToolInvocation invocation;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime = Instant.now();

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "prev_hash", length = 64, nullable = false)
    private String prevHash;

    @Column(name = "curr_hash", length = 64, nullable = false)
    private String currHash;

    @Column(name = "hash_algorithm", length = 32, nullable = false)
    private String hashAlgorithm = "SHA-256";

    @Column(name = "actor", length = 128, nullable = false)
    private String actor;

    public AuditEvent() {}

    public AuditEvent(
            ToolInvocation invocation,
            Long sequenceNumber,
            String eventType,
            String payload,
            String prevHash,
            String currHash,
            String actor) {
        this.invocation = invocation;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.payload = payload;
        this.prevHash = prevHash;
        this.currHash = currHash;
        this.hashAlgorithm = "SHA-256";
        this.actor = actor;
        this.eventTime = Instant.now();
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public ToolInvocation getInvocation() { return invocation; }
    public void setInvocation(ToolInvocation invocation) { this.invocation = invocation; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getCurrHash() { return currHash; }
    public void setCurrHash(String currHash) { this.currHash = currHash; }

    public String getHashAlgorithm() { return hashAlgorithm; }
    public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
