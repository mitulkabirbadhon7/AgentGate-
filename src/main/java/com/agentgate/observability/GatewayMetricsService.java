package com.agentgate.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class GatewayMetricsService {

    private final MeterRegistry meterRegistry;

    public GatewayMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordInvocation(String toolName, String agentId, String status, long latencyMs) {
        Counter.builder("gateway.invocations.total")
                .description("Total number of tool invocations processed by AgentGate")
                .tag("tool", toolName != null ? toolName : "unknown")
                .tag("agent", agentId != null ? agentId : "unknown")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(meterRegistry)
                .increment();

        Timer.builder("gateway.invocation.latency")
                .description("Latency distribution of tool invocations")
                .tag("tool", toolName != null ? toolName : "unknown")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordPolicyCacheAccess(boolean hit) {
        Counter.builder("gateway.policy.cache")
                .tag("result", hit ? "HIT" : "MISS")
                .register(meterRegistry)
                .increment();
    }
}
