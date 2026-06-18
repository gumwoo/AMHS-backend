package org.example.amhs.fab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.common.exception.ErrorCode;

@Entity
@Table(name = "fab_edge")
public class FabEdge {

    @Id
    private String edgeId;

    @Column(nullable = false)
    private String fromNodeId;

    @Column(nullable = false)
    private String toNodeId;

    @Column(nullable = false)
    private double distanceMeters;

    @Column(nullable = false)
    private long estimatedTravelSeconds;

    @Column(nullable = false)
    private boolean blocked;

    protected FabEdge() {
    }

    public FabEdge(
            String edgeId,
            String fromNodeId,
            String toNodeId,
            double distanceMeters,
            long estimatedTravelSeconds,
            boolean blocked
    ) {
        if (distanceMeters <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("distanceMeters", distanceMeters));
        }
        this.edgeId = edgeId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distanceMeters = distanceMeters;
        this.estimatedTravelSeconds = estimatedTravelSeconds;
        this.blocked = blocked;
    }

    public void block() {
        if (blocked) {
            throw new BusinessException(ErrorCode.EDGE_ALREADY_BLOCKED, Map.of("edgeId", edgeId));
        }
        this.blocked = true;
    }

    public void unblock() {
        if (!blocked) {
            throw new BusinessException(ErrorCode.EDGE_ALREADY_UNBLOCKED, Map.of("edgeId", edgeId));
        }
        this.blocked = false;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public long getEstimatedTravelSeconds() {
        return estimatedTravelSeconds;
    }

    public boolean isBlocked() {
        return blocked;
    }
}
