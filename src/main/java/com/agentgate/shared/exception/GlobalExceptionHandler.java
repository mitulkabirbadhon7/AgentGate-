package com.agentgate.shared.exception;

import com.agentgate.shared.api.ApiError;
import com.agentgate.shared.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AgentGateException.class)
    public ResponseEntity<ApiError> handleAgentGateException(AgentGateException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        HttpStatus status = switch (ex.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, POLICY_DENIED, KILL_SWITCH_ACTIVE -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED, INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case RATE_LIMIT_EXCEEDED, QUOTA_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case CIRCUIT_BREAKER_OPEN, SERVICE_UNAVAILABLE, DOWNSTREAM_TIMEOUT -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        log.warn("Handled AgentGateException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(ApiError.of(ex.getErrorCode(), ex.getMessage(), requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(ErrorCode.VALIDATION_FAILED, "Request argument validation failed", requestId, details));
    }

    @ExceptionHandler({
        org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ApiError> handleOptimisticLockException(Exception ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Optimistic locking conflict [requestId={}]: {}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(ErrorCode.CONFLICT,
                        "Concurrent modification detected. The entity was modified by another operation. Please refresh and retry.",
                        requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.error("Unhandled server exception [requestId={}]", requestId, ex);
        // Do not leak stack traces or internal hostnames/credentials to callers
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(ErrorCode.INTERNAL_ERROR, "An internal error occurred. Please refer to request ID: " + requestId, requestId));
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerId = request.getHeader("X-Request-Id");
        return (headerId != null && !headerId.isBlank()) ? headerId : UUID.randomUUID().toString();
    }
}
