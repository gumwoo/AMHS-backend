package org.example.amhs.routing.dto;

import java.util.List;

public record RouteResponse(
        String sourceNodeId,
        String destinationNodeId,
        double totalDistanceMeters,
        long estimatedTravelSeconds,
        List<String> path,
        List<String> edges
) {
}
