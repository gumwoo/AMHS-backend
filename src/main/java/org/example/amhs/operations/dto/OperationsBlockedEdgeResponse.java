package org.example.amhs.operations.dto;

import org.example.amhs.fab.domain.FabEdge;

public record OperationsBlockedEdgeResponse(
        String edgeId,
        String fromNodeId,
        String toNodeId,
        double distanceMeters,
        long estimatedTravelSeconds
) {

    public static OperationsBlockedEdgeResponse from(FabEdge edge) {
        return new OperationsBlockedEdgeResponse(
                edge.getEdgeId(),
                edge.getFromNodeId(),
                edge.getToNodeId(),
                edge.getDistanceMeters(),
                edge.getEstimatedTravelSeconds()
        );
    }
}
