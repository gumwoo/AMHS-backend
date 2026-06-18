package org.example.amhs.transfer.dto;

import java.time.OffsetDateTime;
import org.example.amhs.transfer.domain.TransferHistory;
import org.example.amhs.transfer.domain.TransferRequestStatus;

public record TransferHistoryResponse(
        TransferRequestStatus previousStatus,
        TransferRequestStatus nextStatus,
        String reason,
        OffsetDateTime occurredAt
) {

    public static TransferHistoryResponse from(TransferHistory history) {
        return new TransferHistoryResponse(
                history.getPreviousStatus(),
                history.getNextStatus(),
                history.getReason(),
                history.getOccurredAt()
        );
    }
}
