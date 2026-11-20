package com.agentgate.audit.api;

import com.agentgate.audit.application.AuditChainService;
import com.agentgate.audit.application.AuditDTOs;
import com.agentgate.shared.api.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditAdminController {

    private final AuditChainService auditChainService;

    public AuditAdminController(AuditChainService auditChainService) {
        this.auditChainService = auditChainService;
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<AuditDTOs.AuditEventResponse>>> listEvents(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(auditChainService.listAuditEvents(pageable)));
    }

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<List<AuditDTOs.AuditEventResponse>>> exportAll() {
        return ResponseEntity.ok(ApiResponse.ok("Audit log exported", auditChainService.exportAllEvents()));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuditDTOs.AuditVerificationReport>> verifyChainIntegrity() {
        AuditDTOs.AuditVerificationReport report = auditChainService.verifyAuditIntegrity();
        return ResponseEntity.ok(ApiResponse.ok(report.message(), report));
    }
}
