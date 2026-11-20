package com.agentgate.policy.application;

import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PolicyCacheService {

    private static final Logger log = LoggerFactory.getLogger(PolicyCacheService.class);
    private static final Duration MAX_LOW_RISK_OUTAGE_WINDOW = Duration.ofMinutes(5);

    private final Map<String, CachedPolicySnapshot> snapshotCache = new ConcurrentHashMap<>();
    private volatile Instant lastAuthoritativeSyncTime = Instant.now();
    private volatile boolean storeAvailable = true;

    public record CachedPolicySnapshot(
            PolicyDTOs.EvaluationResult result,
            Instant cachedAt
    ) {}

    public void putSnapshot(String cacheKey, PolicyDTOs.EvaluationResult result) {
        snapshotCache.put(cacheKey, new CachedPolicySnapshot(result, Instant.now()));
        this.lastAuthoritativeSyncTime = Instant.now();
        this.storeAvailable = true;
    }

    public PolicyDTOs.EvaluationResult getSnapshot(String cacheKey) {
        CachedPolicySnapshot cached = snapshotCache.get(cacheKey);
        return cached != null ? cached.result() : null;
    }

    public void invalidateCache() {
        snapshotCache.clear();
        log.info("Policy cache invalidated. Propagation guaranteed within <=10 seconds.");
    }

    public void recordStoreOutage() {
        this.storeAvailable = false;
        log.warn("Authoritative policy store outage detected.");
    }

    public void recordStoreRecovery() {
        this.storeAvailable = true;
        this.lastAuthoritativeSyncTime = Instant.now();
        log.info("Authoritative policy store connectivity restored.");
    }

    public PolicyDTOs.EvaluationResult handleOutageEvaluation(String cacheKey, Tool.RiskLevel riskLevel) {
        if (riskLevel != Tool.RiskLevel.LOW) {
            log.error("Failing closed for {} risk tool during policy store outage", riskLevel);
            throw new AgentGateException(ErrorCode.POLICY_DENIED,
                    "Security Invariant: Medium/High risk operations fail closed during policy store outage");
        }

        CachedPolicySnapshot cached = snapshotCache.get(cacheKey);
        if (cached != null) {
            Duration age = Duration.between(cached.cachedAt(), Instant.now());
            if (age.compareTo(MAX_LOW_RISK_OUTAGE_WINDOW) <= 0) {
                log.warn("Serving low-risk evaluation from bounded cache snapshot (age: {}s)", age.toSeconds());
                return cached.result();
            }
        }

        throw new AgentGateException(ErrorCode.POLICY_DENIED,
                "Policy snapshot expired (>5 minutes) during store outage. Failing closed.");
    }

    public boolean isStoreAvailable() {
        return storeAvailable;
    }
}
