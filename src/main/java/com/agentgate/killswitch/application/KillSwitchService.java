package com.agentgate.killswitch.application;

import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.killswitch.domain.KillSwitch;
import com.agentgate.killswitch.infrastructure.KillSwitchRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KillSwitchService {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchService.class);

    private final KillSwitchRepository killSwitchRepository;
    private final ApprovalRequestRepository approvalRepository;

    // High-performance mirror (<10ms)
    private final ConcurrentHashMap<String, Boolean> localMirror = new ConcurrentHashMap<>();
    private volatile Instant lastMirrorRefresh = Instant.now();

    public KillSwitchService(
            KillSwitchRepository killSwitchRepository,
            ApprovalRequestRepository approvalRepository) {
        this.killSwitchRepository = killSwitchRepository;
        this.approvalRepository = approvalRepository;
    }

    @Transactional
    public KillSwitchDTOs.KillSwitchResponse tripKillSwitch(KillSwitchDTOs.TripKillSwitchRequest request) {
        KillSwitch ks = new KillSwitch(
                request.scope(),
                request.targetId(),
                request.reason(),
                request.adminId()
        );
        ks = killSwitchRepository.save(ks);

        refreshMirror();

        // Automatically cancel affected pending approvals (ADR-0006 & ADR-0008)
        cancelAffectedPendingApprovals(request.scope(), request.targetId(), request.reason());

        log.warn("EMERGENCY KILL SWITCH TRIPPED: scope={}, target={}, reason={}",
                request.scope(), request.targetId(), request.reason());

        return KillSwitchDTOs.KillSwitchResponse.from(ks);
    }

    @Transactional
    public KillSwitchDTOs.KillSwitchResponse resetKillSwitch(Long killSwitchId, String adminId) {
        KillSwitch ks = killSwitchRepository.findById(killSwitchId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Kill switch not found: " + killSwitchId));

        ks.setStatus(KillSwitch.KillSwitchStatus.INACTIVE);
        ks.setDeactivatedAt(Instant.now());
        ks = killSwitchRepository.save(ks);

        refreshMirror();

        log.info("Kill switch {} deactivated by admin {}", killSwitchId, adminId);
        return KillSwitchDTOs.KillSwitchResponse.from(ks);
    }

    @Transactional(readOnly = true)
    public boolean isExecutionBlocked(String agentId, Tool tool) {
        String toolIdStr = (tool != null && tool.getToolId() != null) ? tool.getToolId().toString() : "";
        List<KillSwitch> matches = killSwitchRepository.findActiveMatchingSwitches(agentId, toolIdStr);

        for (KillSwitch ks : matches) {
            if (ks.getScope() == KillSwitch.KillSwitchScope.AGENT && agentId.equals(ks.getTargetId())) {
                return true;
            }
            if (ks.getScope() == KillSwitch.KillSwitchScope.TOOL && toolIdStr.equals(ks.getTargetId())) {
                return true;
            }
            if (ks.getScope() == KillSwitch.KillSwitchScope.GLOBAL_WRITES) {
                if (isWriteOperation(tool)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<KillSwitchDTOs.KillSwitchResponse> listActiveSwitches() {
        return killSwitchRepository.findByStatus(KillSwitch.KillSwitchStatus.ACTIVE).stream()
                .map(KillSwitchDTOs.KillSwitchResponse::from)
                .toList();
    }

    private void cancelAffectedPendingApprovals(KillSwitch.KillSwitchScope scope, String targetId, String reason) {
        List<ApprovalRequest> affected = switch (scope) {
            case AGENT -> approvalRepository.findPendingByAgentId(targetId);
            case TOOL -> {
                try {
                    Long tid = Long.parseLong(targetId);
                    yield approvalRepository.findPendingByToolId(tid);
                } catch (Exception e) {
                    yield List.of();
                }
            }
            case GLOBAL_WRITES -> approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).stream()
                    .filter(ar -> isWriteOperation(ar.getInvocation().getTool()))
                    .toList();
        };

        for (ApprovalRequest ar : affected) {
            ar.setStatus(ApprovalRequest.ApprovalStatus.CANCELLED);
            ar.setDecisionReason("Auto-cancelled due to emergency kill switch trip: " + reason);
            ar.setUpdatedAt(Instant.now());
            approvalRepository.save(ar);
        }
    }

    private boolean isWriteOperation(Tool tool) {
        if (tool == null) return false;
        String hint = tool.getRoutingHint() != null ? tool.getRoutingHint().toUpperCase() : "";
        return hint.startsWith("POST") || hint.startsWith("PUT") || hint.startsWith("DELETE") || hint.startsWith("PATCH");
    }

    private void refreshMirror() {
        localMirror.clear();
        this.lastMirrorRefresh = Instant.now();
    }
}
