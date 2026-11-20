package com.agentgate.invocation.domain;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.domain.EndUser;
import com.agentgate.tool.domain.Tool;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_invocation")
public class ToolInvocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invocation_id")
    private Long invocationId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentClient agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private EndUser user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    @Column(name = "request_arguments_redacted", columnDefinition = "TEXT", nullable = false)
    private String requestArgumentsRedacted;

    @Column(name = "response_payload_redacted", columnDefinition = "TEXT")
    private String responsePayloadRedacted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private InvocationStatus status;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "policy_version_id")
    private Long policyVersionId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public ToolInvocation() {}

    public ToolInvocation(
            UUID eventId,
            AgentClient agent,
            EndUser user,
            Tool tool,
            String requestArgumentsRedacted,
            InvocationStatus status) {
        this.eventId = eventId;
        this.agent = agent;
        this.user = user;
        this.tool = tool;
        this.requestArgumentsRedacted = requestArgumentsRedacted;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public enum InvocationStatus {
        RECEIVED,
        APPROVED,
        EXECUTED,
        BLOCKED,
        REJECTED,
        FAILED,
        UNKNOWN_OUTCOME
    }

    public Long getInvocationId() { return invocationId; }
    public void setInvocationId(Long invocationId) { this.invocationId = invocationId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public AgentClient getAgent() { return agent; }
    public void setAgent(AgentClient agent) { this.agent = agent; }

    public EndUser getUser() { return user; }
    public void setUser(EndUser user) { this.user = user; }

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }

    public String getRequestArgumentsRedacted() { return requestArgumentsRedacted; }
    public void setRequestArgumentsRedacted(String requestArgumentsRedacted) { this.requestArgumentsRedacted = requestArgumentsRedacted; }

    public String getResponsePayloadRedacted() { return responsePayloadRedacted; }
    public void setResponsePayloadRedacted(String responsePayloadRedacted) { this.responsePayloadRedacted = responsePayloadRedacted; }

    public InvocationStatus getStatus() { return status; }
    public void setStatus(InvocationStatus status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Long getPolicyVersionId() { return policyVersionId; }
    public void setPolicyVersionId(Long policyVersionId) { this.policyVersionId = policyVersionId; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
