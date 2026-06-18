package org.example.amhs.oht.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "oht_move_event")
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
            OffsetDateTime occurredAt,
            long travelSeconds
    ) {
        this.eventId = eventId;
        this.ohtId = ohtId;
        this.requestId = requestId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.occurredAt = occurredAt;
        this.travelSeconds = travelSeconds;
    }
}
