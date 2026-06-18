package org.example.amhs.fab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fab_node")
public class FabNode {

    @Id
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double positionX;

    @Column(nullable = false)
    private double positionY;

    @Column(nullable = false)
    private boolean active;

    protected FabNode() {
    }

    public FabNode(
            String nodeId,
            NodeType nodeType,
            String name,
            double positionX,
            double positionY,
            boolean active
    ) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.name = name;
        this.positionX = positionX;
        this.positionY = positionY;
        this.active = active;
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public boolean isActive() {
        return active;
    }
}
