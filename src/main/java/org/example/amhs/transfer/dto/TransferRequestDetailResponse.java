package org.example.amhs.transfer.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record TransferRequestDetailResponse(
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
        String failedReason,
        List<String> path,
        List<TransferHistoryResponse> histories
) {

    public static TransferRequestDetailResponse of(
            TransferRequest request,
            List<String> path,
            List<TransferHistoryResponse> histories
    ) {
        return new TransferRequestDetailResponse(
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
                request.getFailedReason(),
                path,
                histories
        );
    }
}
