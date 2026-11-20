package com.agentgate.approval.api;

import com.agentgate.approval.application.ApprovalDTOs;
import com.agentgate.approval.application.ApprovalService;
import com.agentgate.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalAdminController {

    private final ApprovalService approvalService;

    public ApprovalAdminController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApprovalDTOs.ApprovalResponse>>> listPending() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.listPendingApprovals()));
    }

    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApiResponse<ApprovalDTOs.ApprovalResponse>> approve(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalDTOs.ApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Approval granted", approvalService.approve(approvalId, request)));
    }

    @PostMapping("/{approvalId}/edit-and-approve")
    public ResponseEntity<ApiResponse<ApprovalDTOs.ApprovalResponse>> editAndApprove(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalDTOs.EditAndApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Approval granted with edited arguments",
                approvalService.editAndApprove(approvalId, request)));
    }

    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<ApiResponse<ApprovalDTOs.ApprovalResponse>> reject(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalDTOs.RejectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Approval rejected", approvalService.reject(approvalId, request)));
    }
}
