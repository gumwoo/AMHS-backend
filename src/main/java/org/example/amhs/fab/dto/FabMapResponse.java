package org.example.amhs.fab.dto;

import java.util.List;

public record FabMapResponse(
        List<FabNodeResponse> nodes,
        List<FabEdgeResponse> edges
) {
}
