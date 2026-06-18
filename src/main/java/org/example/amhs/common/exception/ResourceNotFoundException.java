package org.example.amhs.common.exception;

import java.util.Map;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
