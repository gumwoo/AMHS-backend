package org.example.amhs.monitoring.dto;

import java.time.OffsetDateTime;

public record DemoMonitoringStatusResponse(
        boolean running,
        OffsetDateTime startedAt,
        OffsetDateTime lastEventAt,
        long emittedEvents,
        long tickIntervalMs,
        int sseConnections
) {
}
