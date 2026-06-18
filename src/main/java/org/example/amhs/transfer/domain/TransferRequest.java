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
import java.util.Map;
import org.example.amhs.common.exception.InvalidStateTransitionException;

@Entity
@Table(name = "transfer_request")
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Column(nullable = false)
    private String sourceNodeId;

    @Column(nullable = false)
    private String destinationNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferRequestStatus status;

    private String assignedOhtId;
    private OffsetDateTime requestedAt;
    private OffsetDateTime assignedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String failedReason;

    protected TransferRequest() {
    }

    private TransferRequest(
            String sourceNodeId,
            String destinationNodeId,
            TransferPriority priority,
            OffsetDateTime requestedAt
    ) {
        if (sourceNodeId.equals(destinationNodeId)) {
            throw new InvalidStateTransitionException(Map.of(
                    "sourceNodeId", sourceNodeId,
                    "destinationNodeId", destinationNodeId
            ));
        }
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.priority = priority == null ? TransferPriority.NORMAL : priority;
        this.status = TransferRequestStatus.WAITING;
        this.requestedAt = requestedAt;
    }

    public static TransferRequest create(
            String sourceNodeId,
            String destinationNodeId,
            TransferPriority priority,
            OffsetDateTime requestedAt
    ) {
        return new TransferRequest(sourceNodeId, destinationNodeId, priority, requestedAt);
    }

    public void assign(String ohtId, OffsetDateTime now) {
        requireStatus(TransferRequestStatus.WAITING, TransferRequestStatus.ASSIGNED);
        this.status = TransferRequestStatus.ASSIGNED;
        this.assignedOhtId = ohtId;
        this.assignedAt = now;
    }

    public void startMoving(OffsetDateTime now) {
        requireStatus(TransferRequestStatus.ASSIGNED, TransferRequestStatus.MOVING);
        this.status = TransferRequestStatus.MOVING;
        this.startedAt = now;
    }

    public void complete(OffsetDateTime now) {
        requireStatus(TransferRequestStatus.MOVING, TransferRequestStatus.COMPLETED);
        this.status = TransferRequestStatus.COMPLETED;
        this.completedAt = now;
    }

    public void fail(String reason, OffsetDateTime now) {
        if (status != TransferRequestStatus.ASSIGNED && status != TransferRequestStatus.MOVING) {
            throw invalidTransition(TransferRequestStatus.FAILED);
        }
        this.status = TransferRequestStatus.FAILED;
        this.failedReason = reason;
        this.completedAt = now;
    }

    public void cancel(String reason, OffsetDateTime now) {
        if (status != TransferRequestStatus.WAITING && status != TransferRequestStatus.ASSIGNED) {
            throw invalidTransition(TransferRequestStatus.CANCELED);
        }
        this.status = TransferRequestStatus.CANCELED;
        this.failedReason = reason;
        this.completedAt = now;
    }

    private void requireStatus(
            TransferRequestStatus expected,
            TransferRequestStatus nextStatus
    ) {
        if (status != expected) {
            throw invalidTransition(nextStatus);
        }
    }

    private InvalidStateTransitionException invalidTransition(TransferRequestStatus nextStatus) {
        return new InvalidStateTransitionException(Map.of(
                "currentStatus", status.name(),
                "nextStatus", nextStatus.name()
        ));
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getDestinationNodeId() {
        return destinationNodeId;
    }

    public TransferPriority getPriority() {
        return priority;
    }

    public TransferRequestStatus getStatus() {
        return status;
    }

    public String getAssignedOhtId() {
        return assignedOhtId;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public String getFailedReason() {
        return failedReason;
    }
}
