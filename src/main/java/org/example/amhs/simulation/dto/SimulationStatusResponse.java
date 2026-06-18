package org.example.amhs.simulation.dto;

import java.time.OffsetDateTime;

public record SimulationStatusResponse(
        boolean running,
        long activeTransferCount,
        long idleOhtCount,
        long movingOhtCount,
        long errorOhtCount,
        OffsetDateTime lastTickAt
) {
}
