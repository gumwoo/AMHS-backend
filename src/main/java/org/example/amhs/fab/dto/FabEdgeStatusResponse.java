package org.example.amhs.fab.dto;

public record FabEdgeStatusResponse(
        String edgeId,
        boolean blocked,
        String reason
) {
}
