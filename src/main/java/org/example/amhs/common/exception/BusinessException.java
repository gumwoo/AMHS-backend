package org.example.amhs.common.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
