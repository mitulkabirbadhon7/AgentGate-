package com.agentgate.policy.api;

import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyAdminController {

    private final PolicyService policyService;

    public PolicyAdminController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyDTOs.PolicyResponse>> createPolicy(
            @Valid @RequestBody PolicyDTOs.CreatePolicyRequest request) {
        PolicyDTOs.PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Policy created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyDTOs.PolicyResponse>>> listPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(policyService.listPolicies()));
    }

    @PostMapping("/versions")
    public ResponseEntity<ApiResponse<PolicyDTOs.PolicyVersionResponse>> publishVersion(
            @Valid @RequestBody PolicyDTOs.CreateVersionRequest request) {
        PolicyDTOs.PolicyVersionResponse response = policyService.publishVersion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Policy version published successfully", response));
    }

    @GetMapping("/{policyId}/versions")
    public ResponseEntity<ApiResponse<List<PolicyDTOs.PolicyVersionResponse>>> listVersions(
            @PathVariable Long policyId) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.listVersions(policyId)));
    }

    @PostMapping("/bindings")
    public ResponseEntity<ApiResponse<PolicyDTOs.PolicyBindingResponse>> createBinding(
            @Valid @RequestBody PolicyDTOs.CreateBindingRequest request) {
        PolicyDTOs.PolicyBindingResponse response = policyService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Policy binding created successfully", response));
    }

    @GetMapping("/{policyId}/bindings")
    public ResponseEntity<ApiResponse<List<PolicyDTOs.PolicyBindingResponse>>> listBindings(
            @PathVariable Long policyId) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.listBindings(policyId)));
    }

    @PostMapping("/playground/evaluate")
    public ResponseEntity<ApiResponse<PolicyDTOs.EvaluationResult>> evaluatePlayground(
            @Valid @RequestBody PolicyDTOs.EvaluationRequest request) {
        PolicyDTOs.EvaluationResult result = policyService.evaluate(request);
        return ResponseEntity.ok(ApiResponse.ok("Policy evaluation completed", result));
    }
}
