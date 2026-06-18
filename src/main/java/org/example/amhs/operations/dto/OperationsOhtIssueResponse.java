package org.example.amhs.operations.dto;

import java.time.OffsetDateTime;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;

public record OperationsOhtIssueResponse(
        String ohtId,
        OhtStatus status,
        String currentNodeId,
        Long currentRequestId,
        OffsetDateTime lastMovedAt
) {

    public static OperationsOhtIssueResponse from(Oht oht) {
        return new OperationsOhtIssueResponse(
                oht.getOhtId(),
                oht.getStatus(),
                oht.getCurrentNodeId(),
                oht.getCurrentRequestId(),
                oht.getLastMovedAt()
        );
    }
}
