package com.agentgate.quota.application;

import com.agentgate.quota.domain.QuotaPolicy;
import com.agentgate.quota.domain.RateLimitPolicy;
import jakarta.validation.constraints.NotNull;

public class RateLimitDTOs {

    public record CreateRateLimitRequest(
        @NotNull RateLimitPolicy.PolicyScope scope,
        String agentId,
        Long toolId,
        int windowSeconds,
        int maxRequests
    ) {}

    public record CreateQuotaRequest(
        @NotNull RateLimitPolicy.PolicyScope scope,
        String agentId,
        Long toolId,
        @NotNull Long maxAmount,
        String windowUnit
    ) {}

    public record RateLimitResponse(
        Long rateLimitId,
        String scope,
        String agentId,
        Long toolId,
        int windowSeconds,
        int maxRequests
    ) {
        public static RateLimitResponse from(RateLimitPolicy p) {
            return new RateLimitResponse(
                p.getRateLimitId(),
                p.getScope().name(),
                p.getAgent() != null ? p.getAgent().getClientId() : null,
                p.getTool() != null ? p.getTool().getToolId() : null,
                p.getWindowSeconds(),
                p.getMaxRequests()
            );
        }
    }

    public record QuotaResponse(
        Long quotaId,
        String scope,
        String agentId,
        Long toolId,
        Long maxAmount,
        String windowUnit
    ) {
        public static QuotaResponse from(QuotaPolicy q) {
            return new QuotaResponse(
                q.getQuotaId(),
                q.getScope().name(),
                q.getAgent() != null ? q.getAgent().getClientId() : null,
                q.getTool() != null ? q.getTool().getToolId() : null,
                q.getMaxAmount(),
                q.getWindowUnit()
            );
        }
    }
}
