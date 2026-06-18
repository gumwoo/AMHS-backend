package org.example.amhs.dispatch.dto;

public record DispatchCandidateResponse(
        String ohtId,
        String currentNodeId,
        double distanceToSourceMeters,
        long estimatedTravelSeconds
) {
}
