package com.agentgate.tool.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tool")
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "server_id", nullable = false)
    private DownstreamServer server;

    @Column(name = "name", length = 128, nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_schema", columnDefinition = "TEXT", nullable = false)
    private String inputSchema;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 32, nullable = false)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "routing_hint", length = 128)
    private String routingHint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ToolStatus status = ToolStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Tool() {}

    public Tool(DownstreamServer server, String name, String description, String inputSchema, RiskLevel riskLevel, String routingHint) {
        this.server = server;
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        this.routingHint = routingHint;
        this.status = ToolStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ToolStatus {
        ACTIVE,
        DEPRECATED,
        DISABLED
    }

    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }

    public DownstreamServer getServer() { return server; }
    public void setServer(DownstreamServer server) { this.server = server; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputSchema() { return inputSchema; }
    public void setInputSchema(String inputSchema) { this.inputSchema = inputSchema; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getRoutingHint() { return routingHint; }
    public void setRoutingHint(String routingHint) { this.routingHint = routingHint; }

    public ToolStatus getStatus() { return status; }
    public void setStatus(ToolStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
