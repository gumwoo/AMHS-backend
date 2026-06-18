package org.example.amhs.transfer.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record AssignTransferRequestResponse(
        Long requestId,
        TransferRequestStatus status,
        String assignedOhtId,
        OffsetDateTime assignedAt,
        List<String> estimatedPath
) {
}
