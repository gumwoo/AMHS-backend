package org.example.amhs.transfer.dto;

import org.example.amhs.transfer.domain.TransferPriority;

public record CreateTransferRequestRequest(
        String sourceNodeId,
        String destinationNodeId,
        TransferPriority priority
) {
}
