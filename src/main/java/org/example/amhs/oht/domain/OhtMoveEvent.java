package org.example.amhs.oht.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "oht_move_event",
        indexes = {
                @Index(name = "idx_oht_move_event_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_oht_move_event_edge_occurred_at", columnList = "edge_id, occurred_at"),
                @Index(name = "idx_oht_move_event_request_occurred_at", columnList = "request_id, occurred_at")
        }
)
public class OhtMoveEvent {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String ohtId;

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private String fromNodeId;

    @Column(nullable = false)
    private String toNodeId;

    @Column(nullable = false)
    private String edgeId;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false)
    private long travelSeconds;

    protected OhtMoveEvent() {
    }

    public OhtMoveEvent(
            String eventId,
            String ohtId,
            Long requestId,
            String fromNodeId,
            String toNodeId,
            String edgeId,
            OffsetDateTime occurredAt,
            long travelSeconds
    ) {
        this.eventId = eventId;
        this.ohtId = ohtId;
        this.requestId = requestId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.edgeId = edgeId;
        this.occurredAt = occurredAt;
        this.travelSeconds = travelSeconds;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOhtId() {
        return ohtId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public long getTravelSeconds() {
        return travelSeconds;
    }
}
