package org.example.amhs.common.response;

import java.time.OffsetDateTime;

public record ErrorResponse(
        boolean success,
        ErrorBody error,
        OffsetDateTime timestamp
) {

    public static ErrorResponse of(ErrorBody error) {
        return new ErrorResponse(false, error, OffsetDateTime.now());
    }
}
