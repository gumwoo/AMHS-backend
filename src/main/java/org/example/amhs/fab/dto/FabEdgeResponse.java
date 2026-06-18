package org.example.amhs.fab.dto;

import org.example.amhs.fab.domain.FabEdge;

public record FabEdgeResponse(
        String edgeId,
        String fromNodeId,
        String toNodeId,
        double distanceMeters,
        long estimatedTravelSeconds,
        boolean blocked
) {

    public static FabEdgeResponse from(FabEdge edge) {
        return new FabEdgeResponse(
                edge.getEdgeId(),
                edge.getFromNodeId(),
                edge.getToNodeId(),
                edge.getDistanceMeters(),
                edge.getEstimatedTravelSeconds(),
                edge.isBlocked()
        );
    }
}
