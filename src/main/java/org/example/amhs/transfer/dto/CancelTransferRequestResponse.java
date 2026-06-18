package org.example.amhs.transfer.dto;

import org.example.amhs.transfer.domain.TransferRequestStatus;

public record CancelTransferRequestResponse(
        Long requestId,
        TransferRequestStatus status,
        String reason
) {
}
