package com.agentgate.shared.exception;

import com.agentgate.shared.api.ErrorCode;

public class AgentGateException extends RuntimeException {
    private final ErrorCode errorCode;

    public AgentGateException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AgentGateException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
