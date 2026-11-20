package com.agentgate.approval.domain;

import com.agentgate.invocation.domain.ToolInvocation;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_request")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invocation_id", nullable = false, unique = true)
    private ToolInvocation invocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "original_arguments_encrypted", columnDefinition = "TEXT", nullable = false)
    private String originalArgumentsEncrypted;

    @Column(name = "edited_arguments_encrypted", columnDefinition = "TEXT")
    private String editedArgumentsEncrypted;

    @Column(name = "approver_id", length = 128)
    private String approverId;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "arguments_hash", length = 64, nullable = false)
    private String argumentsHash;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ApprovalRequest() {}

    public ApprovalRequest(
            ToolInvocation invocation,
            String originalArgumentsEncrypted,
            String argumentsHash,
            Instant expiresAt) {
        this.invocation = invocation;
        this.originalArgumentsEncrypted = originalArgumentsEncrypted;
        this.argumentsHash = argumentsHash;
        this.expiresAt = expiresAt;
        this.status = ApprovalStatus.PENDING;
        this.version = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }

    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }

    public ToolInvocation getInvocation() { return invocation; }
    public void setInvocation(ToolInvocation invocation) { this.invocation = invocation; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getOriginalArgumentsEncrypted() { return originalArgumentsEncrypted; }
    public void setOriginalArgumentsEncrypted(String originalArgumentsEncrypted) { this.originalArgumentsEncrypted = originalArgumentsEncrypted; }

    public String getEditedArgumentsEncrypted() { return editedArgumentsEncrypted; }
    public void setEditedArgumentsEncrypted(String editedArgumentsEncrypted) { this.editedArgumentsEncrypted = editedArgumentsEncrypted; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public String getArgumentsHash() { return argumentsHash; }
    public void setArgumentsHash(String argumentsHash) { this.argumentsHash = argumentsHash; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
