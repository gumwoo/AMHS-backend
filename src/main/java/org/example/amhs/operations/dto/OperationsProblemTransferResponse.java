package org.example.amhs.operations.dto;

import java.time.OffsetDateTime;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record OperationsProblemTransferResponse(
        Long requestId,
        TransferRequestStatus status,
        TransferPriority priority,
        String sourceNodeId,
        String destinationNodeId,
        String assignedOhtId,
        String reason,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt
) {

    public static OperationsProblemTransferResponse from(TransferRequest request) {
        return new OperationsProblemTransferResponse(
                request.getRequestId(),
                request.getStatus(),
                request.getPriority(),
                request.getSourceNodeId(),
                request.getDestinationNodeId(),
                request.getAssignedOhtId(),
                request.getFailedReason(),
                request.getRequestedAt(),
                request.getCompletedAt()
        );
    }
}
