package org.example.amhs.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transfer_history")
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferRequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferRequestStatus nextStatus;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    protected TransferHistory() {
    }

    public TransferHistory(
            Long requestId,
            TransferRequestStatus previousStatus,
            TransferRequestStatus nextStatus,
            String reason,
            OffsetDateTime occurredAt
    ) {
        this.requestId = requestId;
        this.previousStatus = previousStatus;
        this.nextStatus = nextStatus;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public Long getHistoryId() {
        return historyId;
    }
}
