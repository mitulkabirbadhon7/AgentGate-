package com.agentgate.quota.domain;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.tool.domain.Tool;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rate_limit_policy")
public class RateLimitPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_limit_id")
    private Long rateLimitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 32, nullable = false)
    private PolicyScope scope;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    private AgentClient agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds = 60;

    @Column(name = "max_requests", nullable = false)
    private int maxRequests = 100;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public RateLimitPolicy() {}

    public RateLimitPolicy(PolicyScope scope, AgentClient agent, Tool tool, int windowSeconds, int maxRequests) {
        this.scope = scope;
        this.agent = agent;
        this.tool = tool;
        this.windowSeconds = windowSeconds > 0 ? windowSeconds : 60;
        this.maxRequests = maxRequests > 0 ? maxRequests : 100;
        this.createdAt = Instant.now();
    }

    public enum PolicyScope {
        AGENT,
        TOOL,
        AGENT_TOOL
    }

    public Long getRateLimitId() { return rateLimitId; }
    public void setRateLimitId(Long rateLimitId) { this.rateLimitId = rateLimitId; }

    public PolicyScope getScope() { return scope; }
    public void setScope(PolicyScope scope) { this.scope = scope; }

    public AgentClient getAgent() { return agent; }
    public void setAgent(AgentClient agent) { this.agent = agent; }

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public int getMaxRequests() { return maxRequests; }
    public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
