package org.example.amhs.common.exception;

import java.util.Map;

public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(Map<String, Object> details) {
        super(ErrorCode.INVALID_STATE_TRANSITION, details);
    }
}
