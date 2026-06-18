package org.example.amhs.monitoring.dto;

import java.time.OffsetDateTime;

public record DemoMonitoringActionResponse(
        boolean running,
        String message,
        OffsetDateTime occurredAt,
        long emittedEvents
) {
}
