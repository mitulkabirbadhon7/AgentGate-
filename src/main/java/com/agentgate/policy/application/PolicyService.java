package com.agentgate.policy.application;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyBindingRepository bindingRepository;
    private final AgentClientRepository agentRepository;
    private final ToolRepository toolRepository;
    private final PolicyEvaluator policyEvaluator;
    private final PolicyCacheService policyCacheService;

    public PolicyService(
            PolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyBindingRepository bindingRepository,
            AgentClientRepository agentRepository,
            ToolRepository toolRepository,
            PolicyEvaluator policyEvaluator,
            PolicyCacheService policyCacheService) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.bindingRepository = bindingRepository;
        this.agentRepository = agentRepository;
        this.toolRepository = toolRepository;
        this.policyEvaluator = policyEvaluator;
        this.policyCacheService = policyCacheService;
    }

    @Transactional
    public PolicyDTOs.PolicyResponse createPolicy(PolicyDTOs.CreatePolicyRequest request) {
        if (policyRepository.existsByName(request.name())) {
            throw new AgentGateException(ErrorCode.CONFLICT, "Policy with name '" + request.name() + "' already exists");
        }
        Policy policy = new Policy(request.name());
        policy = policyRepository.save(policy);
        return PolicyDTOs.PolicyResponse.from(policy);
    }

    @Transactional
    public PolicyDTOs.PolicyVersionResponse publishVersion(PolicyDTOs.CreateVersionRequest request) {
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Policy not found: " + request.policyId()));

        Integer maxVersion = versionRepository.findMaxVersionNumber(policy.getPolicyId());
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        String versionHash = computeSha256(request.rulesTree());
        PolicyVersion version = new PolicyVersion(policy, nextVersion, request.rulesTree(), request.auditByUser(), versionHash);
        version = versionRepository.save(version);

        policy.setCurrentVersionId(version.getVersionId());
        policyRepository.save(policy);

        policyCacheService.invalidateCache();
        return PolicyDTOs.PolicyVersionResponse.from(version);
    }

    @Transactional
    public PolicyDTOs.PolicyBindingResponse createBinding(PolicyDTOs.CreateBindingRequest request) {
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Policy not found: " + request.policyId()));

        AgentClient agent = null;
        if (request.agentId() != null && !request.agentId().isBlank()) {
            agent = agentRepository.findById(request.agentId())
                    .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Agent not found: " + request.agentId()));
        }

        Tool tool = null;
        if (request.toolId() != null) {
            tool = toolRepository.findById(request.toolId())
                    .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Tool not found: " + request.toolId()));
        }

        try {
            PolicyBinding.validateScopeConsistency(request.scope(), agent, tool);
        } catch (IllegalArgumentException e) {
            throw new AgentGateException(ErrorCode.VALIDATION_FAILED, e.getMessage());
        }

        PolicyBinding binding = new PolicyBinding(policy, agent, tool, request.scope(), request.priority() > 0 ? request.priority() : 100);
        binding = bindingRepository.save(binding);

        policyCacheService.invalidateCache();
        return PolicyDTOs.PolicyBindingResponse.from(binding);
    }

    @Transactional(readOnly = true)
    public PolicyDTOs.EvaluationResult evaluate(PolicyDTOs.EvaluationRequest request) {
        if (!policyCacheService.isStoreAvailable()) {
            Tool tool = toolRepository.findById(request.toolId()).orElse(null);
            Tool.RiskLevel risk = (tool != null) ? tool.getRiskLevel() : Tool.RiskLevel.HIGH;
            String cacheKey = request.agentId() + ":" + request.toolId();
            return policyCacheService.handleOutageEvaluation(cacheKey, risk);
        }

        PolicyDTOs.EvaluationResult result = policyEvaluator.evaluate(request);
        String cacheKey = request.agentId() + ":" + request.toolId();
        policyCacheService.putSnapshot(cacheKey, result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<PolicyDTOs.PolicyResponse> listPolicies() {
        return policyRepository.findAll().stream()
                .map(PolicyDTOs.PolicyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyDTOs.PolicyVersionResponse> listVersions(Long policyId) {
        return versionRepository.findByPolicy_PolicyIdOrderByVersionNumberDesc(policyId).stream()
                .map(PolicyDTOs.PolicyVersionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyDTOs.PolicyBindingResponse> listBindings(Long policyId) {
        return bindingRepository.findByPolicy_PolicyId(policyId).stream()
                .map(PolicyDTOs.PolicyBindingResponse::from)
                .toList();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256", e);
        }
    }
}
