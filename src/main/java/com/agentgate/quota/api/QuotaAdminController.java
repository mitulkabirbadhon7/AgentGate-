package com.agentgate.quota.api;

import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.quota.application.RateLimitDTOs;
import com.agentgate.quota.application.RateLimitService;
import com.agentgate.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/limits")
public class QuotaAdminController {

    private final RateLimitService rateLimitService;

    public QuotaAdminController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/rate-limits")
    public ResponseEntity<ApiResponse<RateLimitDTOs.RateLimitResponse>> createRateLimit(
            @Valid @RequestBody RateLimitDTOs.CreateRateLimitRequest request) {
        RateLimitDTOs.RateLimitResponse response = rateLimitService.createRateLimit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rate limit policy created successfully", response));
    }

    @GetMapping("/rate-limits")
    public ResponseEntity<ApiResponse<List<RateLimitDTOs.RateLimitResponse>>> listRateLimits() {
        return ResponseEntity.ok(ApiResponse.ok(rateLimitService.listRateLimits()));
    }

    @PostMapping("/quotas")
    public ResponseEntity<ApiResponse<RateLimitDTOs.QuotaResponse>> createQuota(
            @Valid @RequestBody RateLimitDTOs.CreateQuotaRequest request) {
        RateLimitDTOs.QuotaResponse response = rateLimitService.createQuota(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Quota policy created successfully", response));
    }

    @GetMapping("/quotas")
    public ResponseEntity<ApiResponse<List<RateLimitDTOs.QuotaResponse>>> listQuotas() {
        return ResponseEntity.ok(ApiResponse.ok(rateLimitService.listQuotas()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<SecurityAlert>>> listAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(rateLimitService.listRecentAlerts()));
    }
}
