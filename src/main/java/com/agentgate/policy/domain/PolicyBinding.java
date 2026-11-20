package com.agentgate.policy.domain;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.tool.domain.Tool;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "policy_binding")
public class PolicyBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "binding_id")
    private Long bindingId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    private AgentClient agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 32, nullable = false)
    private BindingScope scope;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PolicyBinding() {}

    public PolicyBinding(Policy policy, AgentClient agent, Tool tool, BindingScope scope, int priority) {
        validateScopeConsistency(scope, agent, tool);
        this.policy = policy;
        this.agent = agent;
        this.tool = tool;
        this.scope = scope;
        this.priority = priority;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public enum BindingScope {
        GLOBAL,
        AGENT,
        TOOL,
        AGENT_TOOL
    }

    public static void validateScopeConsistency(BindingScope scope, AgentClient agent, Tool tool) {
        switch (scope) {
            case GLOBAL -> {
                if (agent != null || tool != null) {
                    throw new IllegalArgumentException("GLOBAL scope binding cannot reference agent or tool");
                }
            }
            case AGENT -> {
                if (agent == null || tool != null) {
                    throw new IllegalArgumentException("AGENT scope binding must reference agent and no tool");
                }
            }
            case TOOL -> {
                if (agent != null || tool == null) {
                    throw new IllegalArgumentException("TOOL scope binding must reference tool and no agent");
                }
            }
            case AGENT_TOOL -> {
                if (agent == null || tool == null) {
                    throw new IllegalArgumentException("AGENT_TOOL scope binding must reference both agent and tool");
                }
            }
        }
    }

    public Long getBindingId() { return bindingId; }
    public void setBindingId(Long bindingId) { this.bindingId = bindingId; }

    public Policy getPolicy() { return policy; }
    public void setPolicy(Policy policy) { this.policy = policy; }

    public AgentClient getAgent() { return agent; }
    public void setAgent(AgentClient agent) { this.agent = agent; }

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }

    public BindingScope getScope() { return scope; }
    public void setScope(BindingScope scope) { this.scope = scope; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
