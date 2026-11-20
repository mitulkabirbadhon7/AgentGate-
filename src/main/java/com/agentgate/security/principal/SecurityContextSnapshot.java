package com.agentgate.security.principal;

import java.util.Collections;
import java.util.Set;

public record SecurityContextSnapshot(
    String agentId,
    String userId,
    String clearanceLevel,
    Set<String> scopes
) {
    public SecurityContextSnapshot {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("Agent ID cannot be null or blank in SecurityContextSnapshot");
        }
        scopes = (scopes == null) ? Collections.emptySet() : Collections.unmodifiableSet(scopes);
        clearanceLevel = (clearanceLevel == null) ? "STANDARD" : clearanceLevel;
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
