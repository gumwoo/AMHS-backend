package org.example.amhs.operations.application;

import java.util.List;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.operations.domain.OperationActionLog;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
import org.example.amhs.operations.dto.OperationActionLogResponse;
import org.example.amhs.operations.repository.OperationActionLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Transactional(readOnly = true)
    public List<OperationActionLogResponse> searchLogs(
            String operatorId,
            OperationActionType actionType,
            String targetId,
            int limit
    ) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        Specification<OperationActionLog> spec = buildSearchSpec(
                blankToNull(operatorId),
                actionType,
                blankToNull(targetId)
        );
        return operationActionLogRepository.findAll(
                        spec,
                        PageRequest.of(0, normalizedLimit, Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("actionLogId")
                        ))
                )
                .stream()
                .map(OperationActionLogResponse::from)
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Specification<OperationActionLog> buildSearchSpec(
            String operatorId,
            OperationActionType actionType,
            String targetId
    ) {
        Specification<OperationActionLog> spec = null;
        spec = appendSpec(spec, operatorIdEquals(operatorId));
        spec = appendSpec(spec, actionTypeEquals(actionType));
        spec = appendSpec(spec, targetIdContains(targetId));
        return spec;
    }

    private Specification<OperationActionLog> appendSpec(
            Specification<OperationActionLog> current,
            Specification<OperationActionLog> next
    ) {
        if (next == null) {
            return current;
        }
        return current == null ? next : current.and(next);
    }

    private Specification<OperationActionLog> operatorIdEquals(String operatorId) {
        return operatorId == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("operatorId"), operatorId);
    }

    private Specification<OperationActionLog> actionTypeEquals(OperationActionType actionType) {
        return actionType == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("actionType"), actionType);
    }

    private Specification<OperationActionLog> targetIdContains(String targetId) {
        return targetId == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("targetId")),
                        "%" + targetId.toLowerCase() + "%"
                );
    }
}
