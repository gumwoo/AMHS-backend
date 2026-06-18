package org.example.amhs.simulation.dto;

import java.time.OffsetDateTime;

public record SimulationStartResponse(
        boolean running,
        OffsetDateTime startedAt
) {
}
