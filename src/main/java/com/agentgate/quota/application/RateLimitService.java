package com.agentgate.quota.application;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.alert.application.SecurityAlertService;
import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.quota.domain.QuotaPolicy;
import com.agentgate.quota.domain.RateLimitPolicy;
import com.agentgate.quota.infrastructure.QuotaPolicyRepository;
import com.agentgate.quota.infrastructure.RateLimitPolicyRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.domain.Tool.RiskLevel;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitPolicyRepository rateLimitRepository;
    private final QuotaPolicyRepository quotaRepository;
    private final SecurityAlertService alertService;
    private final AgentClientRepository agentRepository;
    private final ToolRepository toolRepository;
    private final StringRedisTemplate redisTemplate;

    // Fast in-memory atomic token buckets and daily counters
    private final Map<String, WindowCounter> rateLimitCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> dailyQuotaCounters = new ConcurrentHashMap<>();

    private volatile boolean simulateRedisOutage = false;

    private record WindowCounter(AtomicInteger count, long windowStartEpochSec) {}

    public RateLimitService(
            RateLimitPolicyRepository rateLimitRepository,
            QuotaPolicyRepository quotaRepository,
            SecurityAlertService alertService,
            AgentClientRepository agentRepository,
            ToolRepository toolRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.rateLimitRepository = rateLimitRepository;
        this.quotaRepository = quotaRepository;
        this.alertService = alertService;
        this.agentRepository = agentRepository;
        this.toolRepository = toolRepository;
        this.redisTemplate = redisTemplate;
    }

    public void setSimulateRedisOutage(boolean simulate) {
        this.simulateRedisOutage = simulate;
    }

    public void resetCountersForTest() {
        rateLimitCounters.clear();
        dailyQuotaCounters.clear();
    }

    @Transactional
    public void enforceLimitsAndQuotas(String agentId, Tool tool, Map<String, Object> arguments) {
        // ADR-0007 / QA4 outage behavior check
        if (simulateRedisOutage) {
            handleRedisOutage(tool);
        }

        enforceRateLimits(agentId, tool);
        enforceDailyQuota(agentId, tool);
    }

    private void handleRedisOutage(Tool tool) {
        if (tool.getRiskLevel() != RiskLevel.LOW) {
            log.warn("Redis outage detected: failing closed for {} risk tool '{}'", tool.getRiskLevel(), tool.getName());
            throw new AgentGateException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Rate limit store unreachable during outage; failing closed for " + tool.getRiskLevel() + " risk tool");
        }
        log.info("Redis outage detected: permitting fallback to bounded local snapshot for LOW risk tool '{}'", tool.getName());
    }

    private void enforceRateLimits(String agentId, Tool tool) {
        List<RateLimitPolicy> policies = rateLimitRepository.findMatchingPolicies(agentId, tool.getToolId());
        long nowSec = System.currentTimeMillis() / 1000;

        for (RateLimitPolicy policy : policies) {
            String key = "rl:" + policy.getScope() + ":" + agentId + ":" + tool.getToolId();
            
            long currentCount;
            if (redisTemplate != null && !simulateRedisOutage) {
                try {
                    Long val = redisTemplate.opsForValue().increment(key);
                    if (val != null && val == 1L) {
                        redisTemplate.expire(key, Duration.ofSeconds(policy.getWindowSeconds()));
                    }
                    currentCount = val != null ? val : 1L;
                } catch (Exception ex) {
                    log.warn("Redis rate-limit query failed: {}", ex.getMessage());
                    handleRedisOutage(tool);
                    currentCount = incrementLocalCounter(key, nowSec, policy.getWindowSeconds());
                }
            } else {
                currentCount = incrementLocalCounter(key, nowSec, policy.getWindowSeconds());
            }

            if (currentCount > policy.getMaxRequests()) {
                alertService.recordSecurityAlert("RATE_EXCEEDED", "HIGH", agentId,
                        "Rate limit exceeded for agent " + agentId + " on tool " + tool.getName()
                                + ". Allowed: " + policy.getMaxRequests() + " in " + policy.getWindowSeconds() + "s.");
                throw new AgentGateException(ErrorCode.RATE_LIMIT_EXCEEDED,
                        "Rate limit exceeded: maximum " + policy.getMaxRequests() + " requests per " + policy.getWindowSeconds() + " seconds");
            }
        }
    }

    private long incrementLocalCounter(String key, long nowSec, int windowSeconds) {
        WindowCounter counter = rateLimitCounters.compute(key, (k, existing) -> {
            if (existing == null || nowSec - existing.windowStartEpochSec >= windowSeconds) {
                return new WindowCounter(new AtomicInteger(1), nowSec);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return counter.count.get();
    }

    private void enforceDailyQuota(String agentId, Tool tool) {
        List<QuotaPolicy> quotas = quotaRepository.findMatchingQuotas(agentId, tool.getToolId());
        String todayUtc = LocalDate.now(ZoneOffset.UTC).toString();

        for (QuotaPolicy quota : quotas) {
            String key = "quota:" + quota.getScope() + ":" + agentId + ":" + tool.getToolId() + ":" + todayUtc;
            
            long currentUsage;
            if (redisTemplate != null && !simulateRedisOutage) {
                try {
                    Long val = redisTemplate.opsForValue().increment(key);
                    if (val != null && val == 1L) {
                        redisTemplate.expire(key, Duration.ofDays(2));
                    }
                    currentUsage = val != null ? val : 1L;
                } catch (Exception ex) {
                    log.warn("Redis daily-quota query failed: {}", ex.getMessage());
                    handleRedisOutage(tool);
                    AtomicLong counter = dailyQuotaCounters.computeIfAbsent(key, k -> new AtomicLong(0));
                    currentUsage = counter.incrementAndGet();
                }
            } else {
                AtomicLong counter = dailyQuotaCounters.computeIfAbsent(key, k -> new AtomicLong(0));
                currentUsage = counter.incrementAndGet();
            }

            if (currentUsage > quota.getMaxAmount()) {
                alertService.recordSecurityAlert("QUOTA_EXCEEDED", "CRITICAL", agentId,
                        "Daily UTC quota budget exceeded for agent " + agentId + " on tool " + tool.getName()
                                + ". Daily Cap: " + quota.getMaxAmount() + " invocations.");
                throw new AgentGateException(ErrorCode.QUOTA_EXCEEDED,
                        "Daily quota exceeded for UTC date " + todayUtc + ": maximum " + quota.getMaxAmount() + " operations per day");
            }
        }
    }

    @Transactional
    public RateLimitDTOs.RateLimitResponse createRateLimit(RateLimitDTOs.CreateRateLimitRequest request) {
        AgentClient agent = request.agentId() != null ? agentRepository.findById(request.agentId()).orElse(null) : null;
        Tool tool = request.toolId() != null ? toolRepository.findById(request.toolId()).orElse(null) : null;

        RateLimitPolicy policy = new RateLimitPolicy(request.scope(), agent, tool, request.windowSeconds(), request.maxRequests());
        policy = rateLimitRepository.save(policy);
        return RateLimitDTOs.RateLimitResponse.from(policy);
    }

    @Transactional
    public RateLimitDTOs.QuotaResponse createQuota(RateLimitDTOs.CreateQuotaRequest request) {
        AgentClient agent = request.agentId() != null ? agentRepository.findById(request.agentId()).orElse(null) : null;
        Tool tool = request.toolId() != null ? toolRepository.findById(request.toolId()).orElse(null) : null;

        QuotaPolicy quota = new QuotaPolicy(request.scope(), agent, tool, request.maxAmount(), request.windowUnit());
        quota = quotaRepository.save(quota);
        return RateLimitDTOs.QuotaResponse.from(quota);
    }

    @Transactional(readOnly = true)
    public List<RateLimitDTOs.RateLimitResponse> listRateLimits() {
        return rateLimitRepository.findAll().stream()
                .map(RateLimitDTOs.RateLimitResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RateLimitDTOs.QuotaResponse> listQuotas() {
        return quotaRepository.findAll().stream()
                .map(RateLimitDTOs.QuotaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SecurityAlert> listRecentAlerts() {
        return alertService.listRecentAlerts();
    }
}
