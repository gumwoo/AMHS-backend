package org.example.amhs.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "operation_action_log")
public class OperationActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionLogId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationTargetType targetType;

    @Column(nullable = false)
    private String targetId;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected OperationActionLog() {
    }

    public OperationActionLog(
            OperationActionType actionType,
            OperationTargetType targetType,
            String targetId,
            String operatorId,
            String reason,
            OffsetDateTime createdAt
    ) {
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.operatorId = operatorId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getActionLogId() {
        return actionLogId;
    }

    public OperationActionType getActionType() {
        return actionType;
    }

    public OperationTargetType getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
