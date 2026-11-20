package com.agentgate.tool.adapter;

import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.tool.domain.Tool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ToolAdapter {
    boolean supports(Tool tool);
    CompletableFuture<String> execute(Tool tool, Map<String, Object> arguments, SecurityContextSnapshot context);
}
