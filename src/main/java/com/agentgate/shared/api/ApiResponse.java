package com.agentgate.shared.api;

import java.util.Map;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String requestId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Operation successful", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data, String requestId) {
        return new ApiResponse<>(true, message, data, requestId);
    }
}
