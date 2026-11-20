package com.agentgate.shared.api;

import java.util.Map;

public record ApiError(
    boolean success,
    String code,
    String message,
    String requestId,
    Map<String, Object> details
) {
    public static ApiError of(ErrorCode code, String message, String requestId) {
        return new ApiError(false, code.name(), message, requestId, Map.of());
    }

    public static ApiError of(ErrorCode code, String message, String requestId, Map<String, Object> details) {
        return new ApiError(false, code.name(), message, requestId, details);
    }
}
