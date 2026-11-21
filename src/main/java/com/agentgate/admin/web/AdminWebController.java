package com.agentgate.admin.web;

import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.alert.domain.SecurityAlert;
import com.agentgate.alert.infrastructure.SecurityAlertRepository;
import com.agentgate.approval.application.ApprovalDTOs;
import com.agentgate.approval.application.ApprovalService;
import com.agentgate.approval.domain.ApprovalRequest;
import com.agentgate.approval.infrastructure.ApprovalRequestRepository;
import com.agentgate.audit.application.AuditChainService;
import com.agentgate.audit.domain.AuditEvent;
import com.agentgate.audit.infrastructure.AuditEventRepository;
import com.agentgate.killswitch.application.KillSwitchDTOs;
import com.agentgate.killswitch.application.KillSwitchService;
import com.agentgate.killswitch.domain.KillSwitch;
import com.agentgate.killswitch.infrastructure.KillSwitchRepository;
import com.agentgate.policy.application.PolicyDTOs;
import com.agentgate.policy.application.PolicyService;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyRepository;
import com.agentgate.shared.api.ApiResponse;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class AdminWebController {

    private final AgentClientRepository agentRepository;
    private final DownstreamServerRepository serverRepository;
    private final ToolRepository toolRepository;
    private final PolicyService policyService;
    private final PolicyRepository policyRepository;
    private final PolicyBindingRepository bindingRepository;
    private final ApprovalService approvalService;
    private final ApprovalRequestRepository approvalRepository;
    private final AuditChainService auditChainService;
    private final AuditEventRepository auditRepository;
    private final KillSwitchService killSwitchService;
    private final KillSwitchRepository killSwitchRepository;
    private final SecurityAlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    public AdminWebController(
            AgentClientRepository agentRepository,
            DownstreamServerRepository serverRepository,
            ToolRepository toolRepository,
            PolicyService policyService,
            PolicyRepository policyRepository,
            PolicyBindingRepository bindingRepository,
            ApprovalService approvalService,
            ApprovalRequestRepository approvalRepository,
            AuditChainService auditChainService,
            AuditEventRepository auditRepository,
            KillSwitchService killSwitchService,
            KillSwitchRepository killSwitchRepository,
            SecurityAlertRepository alertRepository,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
        this.policyService = policyService;
        this.policyRepository = policyRepository;
        this.bindingRepository = bindingRepository;
        this.approvalService = approvalService;
        this.approvalRepository = approvalRepository;
        this.auditChainService = auditChainService;
        this.auditRepository = auditRepository;
        this.killSwitchService = killSwitchService;
        this.killSwitchRepository = killSwitchRepository;
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper;
    }

    @ModelAttribute("pendingApprovals")
    public long populatePendingApprovals() {
        try {
            return approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).size();
        } catch (Exception e) {
            return 0L;
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        long agentCount = agentRepository.count();
        long toolCount = toolRepository.count();
        long serverCount = serverRepository.count();
        long pendingApprovals = approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).size();
        long totalAuditEvents = auditRepository.count();
        long activeKillSwitches = killSwitchRepository.findByStatus(KillSwitch.KillSwitchStatus.ACTIVE).size();
        boolean globalWritesBlocked = killSwitchRepository.findByStatus(KillSwitch.KillSwitchStatus.ACTIVE)
                .stream().anyMatch(ks -> ks.getScope() == KillSwitch.KillSwitchScope.GLOBAL_WRITES);

        model.addAttribute("agentCount", agentCount);
        model.addAttribute("toolCount", toolCount);
        model.addAttribute("serverCount", serverCount);
        model.addAttribute("pendingApprovals", pendingApprovals);
        model.addAttribute("totalAuditEvents", totalAuditEvents);
        model.addAttribute("activeKillSwitches", activeKillSwitches);
        model.addAttribute("globalWritesBlocked", globalWritesBlocked);
        return "home";
    }

    @GetMapping({"/dashboard", "/admin"})
    public String dashboard(Model model) {
        long agentCount = agentRepository.count();
        long toolCount = toolRepository.count();
        long pendingApprovals = approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).size();
        long totalAuditEvents = auditRepository.count();
        boolean globalWritesBlocked = killSwitchRepository.findByStatus(KillSwitch.KillSwitchStatus.ACTIVE)
                .stream().anyMatch(ks -> ks.getScope() == KillSwitch.KillSwitchScope.GLOBAL_WRITES);

        List<AuditEvent> recentAudits = auditRepository.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "sequenceNumber"))).getContent();
        List<SecurityAlert> recentAlerts = alertRepository.findTop20ByOrderByCreatedAtDesc();

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("agentCount", agentCount);
        model.addAttribute("toolCount", toolCount);
        model.addAttribute("pendingApprovals", pendingApprovals);
        model.addAttribute("totalAuditEvents", totalAuditEvents);
        model.addAttribute("globalWritesBlocked", globalWritesBlocked);
        model.addAttribute("recentAudits", recentAudits);
        model.addAttribute("recentAlerts", recentAlerts);

        return "dashboard";
    }

    @GetMapping("/agents")
    public String agents(Model model) {
        model.addAttribute("activeNav", "agents");
        model.addAttribute("agents", agentRepository.findAll());
        return "agents";
    }

    @GetMapping("/tools")
    public String tools(Model model) {
        model.addAttribute("activeNav", "tools");
        model.addAttribute("servers", serverRepository.findAll());
        model.addAttribute("tools", toolRepository.findAll());
        return "tools";
    }

    @GetMapping("/policies")
    public String policies(Model model) {
        model.addAttribute("activeNav", "policies");
        model.addAttribute("policies", policyRepository.findAll());
        model.addAttribute("bindings", bindingRepository.findAll());
        return "policies";
    }

    @GetMapping("/approvals")
    public String approvals(Model model) {
        model.addAttribute("activeNav", "approvals");
        model.addAttribute("pendingList", approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING));
        model.addAttribute("allApprovals", approvalRepository.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return "approvals";
    }

    @PostMapping("/approvals/decide")
    public String decideApproval(
            @RequestParam Long approvalId,
            @RequestParam String decision,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String editedArgs) {
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : "Decision via Admin Dashboard";
        if ("APPROVE".equalsIgnoreCase(decision)) {
            if (editedArgs != null && !editedArgs.isBlank() && !editedArgs.trim().equals("{}")) {
                try {
                    Map<String, Object> map = objectMapper.readValue(editedArgs, new TypeReference<Map<String, Object>>() {});
                    approvalService.editAndApprove(approvalId, new ApprovalDTOs.EditAndApproveRequest("admin_user", map, effectiveReason));
                } catch (Exception e) {
                    approvalService.approve(approvalId, new ApprovalDTOs.ApproveRequest("admin_user", effectiveReason));
                }
            } else {
                approvalService.approve(approvalId, new ApprovalDTOs.ApproveRequest("admin_user", effectiveReason));
            }
        } else {
            approvalService.reject(approvalId, new ApprovalDTOs.RejectRequest("admin_user", effectiveReason));
        }
        return "redirect:/approvals";
    }

    @GetMapping("/audit")
    public String audit(Model model, @RequestParam(required = false) String verify) {
        model.addAttribute("activeNav", "audit");
        List<AuditEvent> events = auditRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "sequenceNumber"))).getContent();
        model.addAttribute("events", events);

        if ("true".equalsIgnoreCase(verify)) {
            var verification = auditChainService.verifyAuditIntegrity();
            model.addAttribute("verificationResult", verification);
        }

        return "audit";
    }

    @GetMapping("/killswitches")
    public String killSwitches(Model model) {
        model.addAttribute("activeNav", "killswitches");
        model.addAttribute("killSwitches", killSwitchRepository.findAll());
        model.addAttribute("agents", agentRepository.findAll());
        model.addAttribute("tools", toolRepository.findAll());
        return "killswitches";
    }

    @PostMapping("/killswitches/toggle")
    public String toggleKillSwitch(
            @RequestParam String scope,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String reason) {
        KillSwitch.KillSwitchScope ksScope = KillSwitch.KillSwitchScope.valueOf(scope);
        String cleanTargetId = (targetId != null && !targetId.isBlank()) ? targetId.trim() : null;
        killSwitchService.tripKillSwitch(new KillSwitchDTOs.TripKillSwitchRequest(
                ksScope,
                cleanTargetId,
                reason != null && !reason.isBlank() ? reason.trim() : "Emergency activation via Admin UI",
                "admin_user"
        ));
        return "redirect:/killswitches";
    }

    @PostMapping("/killswitches/reset")
    public String resetKillSwitch(
            @RequestParam Long killSwitchId,
            @RequestParam(required = false) String reason) {
        killSwitchService.resetKillSwitch(killSwitchId, "admin_user");
        return "redirect:/killswitches";
    }

    @PostMapping("/policies/playground/evaluate")
    @ResponseBody
    public ResponseEntity<ApiResponse<PolicyDTOs.EvaluationResult>> evaluatePlayground(
            @RequestBody PolicyDTOs.EvaluationRequest request) {
        PolicyDTOs.EvaluationResult result = policyService.evaluate(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
