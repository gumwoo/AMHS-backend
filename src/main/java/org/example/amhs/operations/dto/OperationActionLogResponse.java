package org.example.amhs.operations.dto;

import java.time.OffsetDateTime;
import org.example.amhs.operations.domain.OperationActionLog;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;

public record OperationActionLogResponse(
        Long actionLogId,
        OperationActionType actionType,
        OperationTargetType targetType,
        String targetId,
        String operatorId,
        String reason,
        OffsetDateTime createdAt
) {

    public static OperationActionLogResponse from(OperationActionLog log) {
        return new OperationActionLogResponse(
                log.getActionLogId(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getOperatorId(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
