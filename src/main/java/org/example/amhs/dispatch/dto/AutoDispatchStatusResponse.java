package org.example.amhs.dispatch.dto;

import java.time.OffsetDateTime;

public record AutoDispatchStatusResponse(
        boolean running,
        int scanLimit,
        OffsetDateTime lastTickAt,
        int lastScannedCount,
        int lastAssignedCount,
        int lastSkippedCount
) {
}
