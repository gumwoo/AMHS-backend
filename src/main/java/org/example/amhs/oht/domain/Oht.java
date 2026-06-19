package org.example.amhs.oht.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import org.example.amhs.common.exception.InvalidStateTransitionException;

@Entity
@Table(name = "oht")
public class Oht {

    @Id
    private String ohtId;

    @Column(nullable = false)
    private String currentNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OhtStatus status;

    private Long currentRequestId;
    private String carryingFoupId;
    private OffsetDateTime lastMovedAt;

    protected Oht() {
    }

    private Oht(String ohtId, String currentNodeId, OffsetDateTime lastMovedAt) {
        this.ohtId = ohtId;
        this.currentNodeId = currentNodeId;
        this.status = OhtStatus.IDLE;
        this.lastMovedAt = lastMovedAt;
    }

    public static Oht create(String ohtId, String currentNodeId, OffsetDateTime lastMovedAt) {
        return new Oht(ohtId, currentNodeId, lastMovedAt);
    }

    public void reserve(Long requestId, OffsetDateTime now) {
        if (status != OhtStatus.IDLE || currentRequestId != null) {
            throw invalidTransition(OhtStatus.RESERVED);
        }
        this.status = OhtStatus.RESERVED;
        this.currentRequestId = requestId;
        this.lastMovedAt = now;
    }

    public void startMoving(OffsetDateTime now) {
        if (status != OhtStatus.RESERVED) {
            throw invalidTransition(OhtStatus.MOVING);
        }
        this.status = OhtStatus.MOVING;
        this.lastMovedAt = now;
    }

    public void moveTo(String nextNodeId, OffsetDateTime now) {
        if (status != OhtStatus.RESERVED && status != OhtStatus.MOVING) {
            throw invalidTransition(OhtStatus.MOVING);
        }
        this.currentNodeId = nextNodeId;
        this.lastMovedAt = now;
    }

    public void load(String foupId, OffsetDateTime now) {
        if (status != OhtStatus.RESERVED && status != OhtStatus.MOVING) {
            throw invalidTransition(OhtStatus.MOVING);
        }
        this.carryingFoupId = foupId;
        this.lastMovedAt = now;
    }

    public void release(OffsetDateTime now) {
        if (status != OhtStatus.RESERVED && status != OhtStatus.MOVING) {
            throw invalidTransition(OhtStatus.IDLE);
        }
        this.status = OhtStatus.IDLE;
        this.currentRequestId = null;
        this.carryingFoupId = null;
        this.lastMovedAt = now;
    }

    public void markError(OffsetDateTime now) {
        this.status = OhtStatus.ERROR;
        this.lastMovedAt = now;
    }

    public void recover(OffsetDateTime now) {
        if (status != OhtStatus.ERROR) {
            throw invalidTransition(OhtStatus.IDLE);
        }
        this.status = OhtStatus.IDLE;
        this.currentRequestId = null;
        this.carryingFoupId = null;
        this.lastMovedAt = now;
    }

    private InvalidStateTransitionException invalidTransition(OhtStatus nextStatus) {
        return new InvalidStateTransitionException(Map.of(
                "currentStatus", status.name(),
                "nextStatus", nextStatus.name()
        ));
    }

    public String getOhtId() {
        return ohtId;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public OhtStatus getStatus() {
        return status;
    }

    public Long getCurrentRequestId() {
        return currentRequestId;
    }

    public String getCarryingFoupId() {
        return carryingFoupId;
    }

    public OffsetDateTime getLastMovedAt() {
        return lastMovedAt;
    }
}
