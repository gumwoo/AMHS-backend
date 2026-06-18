package org.example.amhs.oht.dto;

import java.time.OffsetDateTime;

public record RecentMoveEventResponse(
        String fromNodeId,
        String toNodeId,
        OffsetDateTime occurredAt,
        long travelSeconds
) {
}
