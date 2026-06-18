package org.example.amhs.fab.dto;

import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;

public record FabNodeResponse(
        String nodeId,
        NodeType nodeType,
        String name,
        double positionX,
        double positionY,
        boolean active
) {

    public static FabNodeResponse from(FabNode node) {
        return new FabNodeResponse(
                node.getNodeId(),
                node.getNodeType(),
                node.getName(),
                node.getPositionX(),
                node.getPositionY(),
                node.isActive()
        );
    }
}
