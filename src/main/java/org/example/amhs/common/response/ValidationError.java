package org.example.amhs.common.response;

public record ValidationError(
        String field,
        String message,
        Object rejectedValue
) {
}
