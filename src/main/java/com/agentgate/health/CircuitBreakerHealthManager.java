package com.agentgate.health;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CircuitBreakerHealthManager {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerHealthManager.class);
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final Duration WAIT_DURATION_OPEN = Duration.ofSeconds(10);

    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public static class CircuitInfo {
        CircuitState state = CircuitState.CLOSED;
        AtomicInteger consecutiveFailures = new AtomicInteger(0);
        Instant openTimestamp = null;

        public CircuitState getState() { return state; }
        public int getConsecutiveFailures() { return consecutiveFailures.get(); }
    }

    private final Map<Long, CircuitInfo> toolCircuits = new ConcurrentHashMap<>();

    public boolean isHealthy(Tool tool) {
        if (tool == null || tool.getToolId() == null) return false;
        CircuitInfo info = toolCircuits.get(tool.getToolId());
        if (info == null) return true;

        if (info.state == CircuitState.OPEN) {
            if (info.openTimestamp != null &&
                Duration.between(info.openTimestamp, Instant.now()).compareTo(WAIT_DURATION_OPEN) >= 0) {
                info.state = CircuitState.HALF_OPEN;
                log.info("Circuit breaker for tool '{}' transitioning from OPEN to HALF_OPEN probe state", tool.getName());
                return true;
            }
            return false;
        }
        return true;
    }

    public void verifyExecutionPermitted(Tool tool) {
        if (!isHealthy(tool)) {
            throw new AgentGateException(ErrorCode.CIRCUIT_BREAKER_OPEN,
                    "Circuit breaker is OPEN for tool '" + tool.getName() + "'. Tool is temporarily unavailable.");
        }
    }

    public void recordSuccess(Tool tool) {
        if (tool == null || tool.getToolId() == null) return;
        CircuitInfo info = toolCircuits.computeIfAbsent(tool.getToolId(), k -> new CircuitInfo());
        info.consecutiveFailures.set(0);
        if (info.state != CircuitState.CLOSED) {
            info.state = CircuitState.CLOSED;
            log.info("Circuit breaker for tool '{}' recovered to CLOSED state", tool.getName());
        }
    }

    public void recordFailure(Tool tool) {
        if (tool == null || tool.getToolId() == null) return;
        CircuitInfo info = toolCircuits.computeIfAbsent(tool.getToolId(), k -> new CircuitInfo());
        int fails = info.consecutiveFailures.incrementAndGet();

        int threshold = (tool.getServer() != null && tool.getServer().getCircuitOpenThreshold() > 0)
                ? tool.getServer().getCircuitOpenThreshold() : DEFAULT_FAILURE_THRESHOLD;

        if (fails >= threshold && info.state != CircuitState.OPEN) {
            info.state = CircuitState.OPEN;
            info.openTimestamp = Instant.now();
            log.warn("CIRCUIT BREAKER TRIPPED to OPEN for tool '{}' after {} consecutive failures", tool.getName(), fails);
        }
    }

    public CircuitState getState(Tool tool) {
        if (tool == null || tool.getToolId() == null) return CircuitState.CLOSED;
        CircuitInfo info = toolCircuits.get(tool.getToolId());
        return info != null ? info.state : CircuitState.CLOSED;
    }

    public void reset(Tool tool) {
        if (tool != null && tool.getToolId() != null) {
            toolCircuits.remove(tool.getToolId());
        }
    }

    public void forceState(Tool tool, CircuitState state, Instant timestamp) {
        if (tool != null && tool.getToolId() != null) {
            CircuitInfo info = toolCircuits.computeIfAbsent(tool.getToolId(), k -> new CircuitInfo());
            info.state = state;
            info.openTimestamp = timestamp;
        }
    }
}
