package org.example.amhs.common.response;

import java.util.Map;
import org.example.amhs.common.exception.ErrorCode;

public record ErrorBody(
        ErrorCode code,
        String message,
        Map<String, Object> details,
        String traceId
) {
}
