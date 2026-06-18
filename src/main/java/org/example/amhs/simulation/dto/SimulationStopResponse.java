package org.example.amhs.simulation.dto;

import java.time.OffsetDateTime;

public record SimulationStopResponse(
        boolean running,
        OffsetDateTime stoppedAt
) {
}
