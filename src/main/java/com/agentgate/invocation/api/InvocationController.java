package com.agentgate.invocation.api;

import com.agentgate.invocation.application.InvocationDTOs;
import com.agentgate.invocation.application.InvocationService;
import com.agentgate.invocation.domain.ToolInvocation;
import com.agentgate.invocation.infrastructure.ToolInvocationRepository;
import com.agentgate.security.filter.AgentAuthenticationFilter;
import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ApiResponse;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invocations")
public class InvocationController {

    private final InvocationService invocationService;
    private final ToolInvocationRepository invocationRepository;

    public InvocationController(
            InvocationService invocationService,
            ToolInvocationRepository invocationRepository) {
        this.invocationService = invocationService;
        this.invocationRepository = invocationRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvocationDTOs.InvocationResponse>> invokeTool(
            @Valid @RequestBody InvocationDTOs.InvokeToolRequest request,
            HttpServletRequest servletRequest) {

        SecurityContextSnapshot securityContext = (SecurityContextSnapshot)
                servletRequest.getAttribute(AgentAuthenticationFilter.SECURITY_CONTEXT_ATTR);

        if (securityContext == null) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Caller must be authenticated via Bearer token");
        }

        InvocationDTOs.InvocationResponse response = invocationService.invokeTool(request, securityContext);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvocationDTOs.InvocationResponse>>> listRecentInvocations(
            @RequestParam(defaultValue = "50") int limit) {
        int pageLimit = Math.min(Math.max(limit, 1), 100);
        List<InvocationDTOs.InvocationResponse> invocations = invocationRepository.findAll(
                PageRequest.of(0, pageLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(InvocationDTOs.InvocationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(invocations));
    }

    @GetMapping("/{invocationId}")
    public ResponseEntity<ApiResponse<InvocationDTOs.InvocationResponse>> getInvocation(
            @PathVariable Long invocationId) {
        ToolInvocation invocation = invocationRepository.findById(invocationId)
                .orElseThrow(() -> new AgentGateException(ErrorCode.RESOURCE_NOT_FOUND, "Invocation not found: " + invocationId));

        return ResponseEntity.ok(ApiResponse.ok(InvocationDTOs.InvocationResponse.from(invocation)));
    }
}
