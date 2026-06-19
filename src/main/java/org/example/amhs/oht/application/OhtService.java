package org.example.amhs.oht.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.monitoring.application.MonitoringEventService;
import org.example.amhs.monitoring.event.DomainEventType;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.dto.OhtDetailResponse;
import org.example.amhs.oht.dto.OhtResponse;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.operations.application.OperationActionLogService;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OhtService {

    private final OhtRepository ohtRepository;
    private final TimeProvider timeProvider;
    private final MonitoringEventService monitoringEventService;
    private final OperationActionLogService operationActionLogService;

    public OhtService(
            OhtRepository ohtRepository,
            TimeProvider timeProvider,
            MonitoringEventService monitoringEventService,
            OperationActionLogService operationActionLogService
    ) {
        this.ohtRepository = ohtRepository;
        this.timeProvider = timeProvider;
        this.monitoringEventService = monitoringEventService;
        this.operationActionLogService = operationActionLogService;
    }

    @Transactional(readOnly = true)
    public List<OhtResponse> getOhts(OhtStatus status, String currentNodeId) {
        return ohtRepository.findAll().stream()
                .filter(oht -> status == null || oht.getStatus() == status)
                .filter(oht -> currentNodeId == null || oht.getCurrentNodeId().equals(currentNodeId))
                .sorted(Comparator.comparing(oht -> oht.getOhtId()))
                .map(OhtResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OhtDetailResponse getOht(String ohtId) {
        return ohtRepository.findById(ohtId)
                .map(OhtDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.OHT_NOT_FOUND,
                        Map.of("ohtId", ohtId)
                ));
    }

    @Transactional
    public OhtResponse markError(String ohtId) {
        return markError(ohtId, null);
    }

    @Transactional
    public OhtResponse markError(String ohtId, String operatorId) {
        Oht oht = getOhtEntity(ohtId);
        var now = timeProvider.now();
        oht.markError(now);
        operationActionLogService.record(
                OperationActionType.OHT_MARKED_ERROR,
                OperationTargetType.OHT,
                ohtId,
                operatorId,
                "운영자 오류 처리"
        );
        monitoringEventService.publishAfterCommit(
                DomainEventType.OHT_ERROR_OCCURRED,
                now,
                Map.of(
                        "ohtId", oht.getOhtId(),
                        "currentNodeId", oht.getCurrentNodeId(),
                        "requestId", oht.getCurrentRequestId() == null ? "-" : oht.getCurrentRequestId(),
                        "errorCode", "OPERATOR_MARKED_ERROR"
                )
        );
        return OhtResponse.from(oht);
    }

    @Transactional
    public OhtResponse recover(String ohtId) {
        return recover(ohtId, null);
    }

    @Transactional
    public OhtResponse recover(String ohtId, String operatorId) {
        Oht oht = getOhtEntity(ohtId);
        var now = timeProvider.now();
        oht.recover(now);
        operationActionLogService.record(
                OperationActionType.OHT_RECOVERED,
                OperationTargetType.OHT,
                ohtId,
                operatorId,
                "운영자 복구 처리"
        );
        monitoringEventService.publishAfterCommit(
                DomainEventType.OHT_RECOVERED,
                now,
                Map.of(
                        "ohtId", oht.getOhtId(),
                        "currentNodeId", oht.getCurrentNodeId()
                )
        );
        return OhtResponse.from(oht);
    }

    private Oht getOhtEntity(String ohtId) {
        return ohtRepository.findById(ohtId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.OHT_NOT_FOUND,
                        Map.of("ohtId", ohtId)
                ));
    }
}
