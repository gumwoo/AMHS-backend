package org.example.amhs.operations.application;

import java.util.List;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.operations.domain.OperationActionLog;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
import org.example.amhs.operations.dto.OperationActionLogResponse;
import org.example.amhs.operations.repository.OperationActionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationActionLogService {

    private static final String DEFAULT_OPERATOR_ID = "operator01";

    private final OperationActionLogRepository operationActionLogRepository;
    private final TimeProvider timeProvider;

    public OperationActionLogService(
            OperationActionLogRepository operationActionLogRepository,
            TimeProvider timeProvider
    ) {
        this.operationActionLogRepository = operationActionLogRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public OperationActionLogResponse record(
            OperationActionType actionType,
            OperationTargetType targetType,
            String targetId,
            String operatorId,
            String reason
    ) {
        OperationActionLog log = operationActionLogRepository.save(new OperationActionLog(
                actionType,
                targetType,
                targetId,
                normalizeOperatorId(operatorId),
                reason == null || reason.isBlank() ? "-" : reason,
                timeProvider.now()
        ));
        return OperationActionLogResponse.from(log);
    }

    private String normalizeOperatorId(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? DEFAULT_OPERATOR_ID : operatorId.trim();
    }

    @Transactional(readOnly = true)
    public List<OperationActionLogResponse> getRecentLogs() {
        return operationActionLogRepository.findTop20ByOrderByCreatedAtDescActionLogIdDesc()
                .stream()
                .map(OperationActionLogResponse::from)
                .toList();
    }
}
