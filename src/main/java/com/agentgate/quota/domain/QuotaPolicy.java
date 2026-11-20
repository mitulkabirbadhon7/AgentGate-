package com.agentgate.quota.domain;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.tool.domain.Tool;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quota_policy")
public class QuotaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quota_id")
    private Long quotaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 32, nullable = false)
    private RateLimitPolicy.PolicyScope scope;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    private AgentClient agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @Column(name = "max_amount", nullable = false)
    private Long maxAmount;

    @Column(name = "window_unit", length = 32, nullable = false)
    private String windowUnit = "DAILY_UTC";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public QuotaPolicy() {}

    public QuotaPolicy(RateLimitPolicy.PolicyScope scope, AgentClient agent, Tool tool, Long maxAmount, String windowUnit) {
        this.scope = scope;
        this.agent = agent;
        this.tool = tool;
        this.maxAmount = maxAmount;
        this.windowUnit = windowUnit != null ? windowUnit : "DAILY_UTC";
        this.createdAt = Instant.now();
    }

    public Long getQuotaId() { return quotaId; }
    public void setQuotaId(Long quotaId) { this.quotaId = quotaId; }

    public RateLimitPolicy.PolicyScope getScope() { return scope; }
    public void setScope(RateLimitPolicy.PolicyScope scope) { this.scope = scope; }

    public AgentClient getAgent() { return agent; }
    public void setAgent(AgentClient agent) { this.agent = agent; }

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }

    public Long getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Long maxAmount) { this.maxAmount = maxAmount; }

    public String getWindowUnit() { return windowUnit; }
    public void setWindowUnit(String windowUnit) { this.windowUnit = windowUnit; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
