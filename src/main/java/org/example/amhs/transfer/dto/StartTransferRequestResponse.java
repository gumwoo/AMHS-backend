package org.example.amhs.transfer.dto;

import java.time.OffsetDateTime;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record StartTransferRequestResponse(
        Long requestId,
        TransferRequestStatus status,
        String assignedOhtId,
        OffsetDateTime startedAt
) {
}
