package org.example.amhs.routing.domain;

import java.util.List;
import org.example.amhs.routing.dto.RouteResponse;

public record RouteResult(
        String sourceNodeId,
        String destinationNodeId,
        List<String> pathNodeIds,
        List<String> pathEdgeIds,
        double totalDistanceMeters,
        long estimatedTravelSeconds
) {

    public RouteResponse toResponse() {
        return new RouteResponse(
                sourceNodeId,
                destinationNodeId,
                totalDistanceMeters,
                estimatedTravelSeconds,
                pathNodeIds,
                pathEdgeIds
        );
    }
}
