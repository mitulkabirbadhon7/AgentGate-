package com.agentgate.tool.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "downstream_server")
public class DownstreamServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "name", length = 128, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private ServerType type;

    @Column(name = "endpoint_url", length = 512, nullable = false)
    private String endpointUrl;

    @Column(name = "health_check_interval_sec", nullable = false)
    private int healthCheckIntervalSec = 30;

    @Column(name = "circuit_open_threshold", nullable = false)
    private int circuitOpenThreshold = 5;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public DownstreamServer() {}

    public DownstreamServer(String name, ServerType type, String endpointUrl, int healthCheckIntervalSec, int circuitOpenThreshold) {
        this.name = name;
        this.type = type;
        this.endpointUrl = endpointUrl;
        this.healthCheckIntervalSec = healthCheckIntervalSec;
        this.circuitOpenThreshold = circuitOpenThreshold;
        this.createdAt = Instant.now();
    }

    public enum ServerType {
        MCP,
        REST
    }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ServerType getType() { return type; }
    public void setType(ServerType type) { this.type = type; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public int getHealthCheckIntervalSec() { return healthCheckIntervalSec; }
    public void setHealthCheckIntervalSec(int healthCheckIntervalSec) { this.healthCheckIntervalSec = healthCheckIntervalSec; }

    public int getTimeoutSeconds() { return healthCheckIntervalSec; }

    public int getCircuitOpenThreshold() { return circuitOpenThreshold; }
    public void setCircuitOpenThreshold(int circuitOpenThreshold) { this.circuitOpenThreshold = circuitOpenThreshold; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
