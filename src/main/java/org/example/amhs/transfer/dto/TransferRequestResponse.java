package org.example.amhs.transfer.dto;

import java.time.OffsetDateTime;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record TransferRequestResponse(
        Long requestId,
        String sourceNodeId,
        String destinationNodeId,
        TransferPriority priority,
        TransferRequestStatus status,
        String assignedOhtId,
        OffsetDateTime requestedAt,
        OffsetDateTime assignedAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String failedReason
) {

    public static TransferRequestResponse from(TransferRequest request) {
        return new TransferRequestResponse(
                request.getRequestId(),
                request.getSourceNodeId(),
                request.getDestinationNodeId(),
                request.getPriority(),
                request.getStatus(),
                request.getAssignedOhtId(),
                request.getRequestedAt(),
                request.getAssignedAt(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getFailedReason()
        );
    }
}
