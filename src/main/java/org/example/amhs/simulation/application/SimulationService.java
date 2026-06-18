package org.example.amhs.simulation.application;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.amhs.common.config.AmhsProperties;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.monitoring.application.MonitoringEventService;
import org.example.amhs.monitoring.event.DomainEventType;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtMoveEvent;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtMoveEventRepository;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.routing.application.RoutingService;
import org.example.amhs.routing.domain.RouteResult;
import org.example.amhs.simulation.dto.SimulationStartResponse;
import org.example.amhs.simulation.dto.SimulationStatusResponse;
import org.example.amhs.simulation.dto.SimulationStopResponse;
import org.example.amhs.transfer.domain.TransferHistory;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private OffsetDateTime startedAt;
    private OffsetDateTime stoppedAt;
    private OffsetDateTime lastTickAt;

    private final TransferRequestRepository transferRequestRepository;
    private final TransferHistoryRepository transferHistoryRepository;
    private final OhtRepository ohtRepository;
    private final OhtMoveEventRepository ohtMoveEventRepository;
    private final RoutingService routingService;
    private final TimeProvider timeProvider;
    private final MonitoringEventService monitoringEventService;
    private final AmhsProperties amhsProperties;

    public SimulationService(
            TransferRequestRepository transferRequestRepository,
            TransferHistoryRepository transferHistoryRepository,
            OhtRepository ohtRepository,
            OhtMoveEventRepository ohtMoveEventRepository,
            RoutingService routingService,
            TimeProvider timeProvider,
            MonitoringEventService monitoringEventService,
            AmhsProperties amhsProperties
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.transferHistoryRepository = transferHistoryRepository;
        this.ohtRepository = ohtRepository;
        this.ohtMoveEventRepository = ohtMoveEventRepository;
        this.routingService = routingService;
        this.timeProvider = timeProvider;
        this.monitoringEventService = monitoringEventService;
        this.amhsProperties = amhsProperties;
    }

    public SimulationStartResponse start() {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.SIMULATION_ALREADY_RUNNING);
        }
        startedAt = timeProvider.now();
        stoppedAt = null;
        return new SimulationStartResponse(true, startedAt);
    }

    public SimulationStopResponse stop() {
        if (!running.compareAndSet(true, false)) {
            throw new BusinessException(ErrorCode.SIMULATION_NOT_RUNNING);
        }
        stoppedAt = timeProvider.now();
        return new SimulationStopResponse(false, stoppedAt);
    }

    @Transactional(readOnly = true)
    public SimulationStatusResponse getStatus() {
        long movingRequests = transferRequestRepository.countByStatus(TransferRequestStatus.MOVING);
        long assignedRequests = transferRequestRepository.countByStatus(TransferRequestStatus.ASSIGNED);
        return new SimulationStatusResponse(
                running.get(),
                movingRequests + assignedRequests,
                ohtRepository.countByStatus(OhtStatus.IDLE),
                ohtRepository.countByStatus(OhtStatus.MOVING),
                ohtRepository.countByStatus(OhtStatus.ERROR),
                lastTickAt
        );
    }

    @Transactional
    public int tickOnce() {
        lastTickAt = timeProvider.now();
        List<TransferRequest> movingRequests = transferRequestRepository.findByStatus(TransferRequestStatus.MOVING);
        int processedCount = 0;
        for (TransferRequest request : movingRequests) {
            processMovingRequest(request, lastTickAt);
            processedCount++;
        }
        return processedCount;
    }

    private void processMovingRequest(TransferRequest request, OffsetDateTime now) {
        if (request.getAssignedOhtId() == null) {
            failRequest(request, "ASSIGNED_OHT_NOT_FOUND", now);
            return;
        }
        Oht oht = ohtRepository.findById(request.getAssignedOhtId())
                .orElse(null);
        if (oht == null) {
            failRequest(request, "OHT_NOT_FOUND", now);
            return;
        }
        if (oht.getStatus() == OhtStatus.ERROR) {
            failRequest(request, "OHT_ERROR_OCCURRED", now);
            return;
        }

        String targetNodeId = oht.getCurrentNodeId().equals(request.getSourceNodeId())
                ? request.getDestinationNodeId()
                : request.getSourceNodeId();
        RouteResult route;
        try {
            route = routingService.findShortestPath(oht.getCurrentNodeId(), targetNodeId);
        } catch (BusinessException exception) {
            failRequest(request, "ROUTE_NOT_FOUND", now);
            return;
        }
        List<String> path = route.pathNodeIds();
        if (path.size() <= 1) {
            if (targetNodeId.equals(request.getDestinationNodeId())) {
                completeRequest(request, oht, now);
            }
            return;
        }

        String fromNodeId = oht.getCurrentNodeId();
        String toNodeId = path.get(1);
        String edgeId = route.pathEdgeIds().isEmpty() ? "UNKNOWN_EDGE" : route.pathEdgeIds().get(0);
        oht.moveTo(toNodeId, now);
        ohtMoveEventRepository.save(new OhtMoveEvent(
                "evt_" + UUID.randomUUID(),
                oht.getOhtId(),
                request.getRequestId(),
                fromNodeId,
                toNodeId,
                edgeId,
                now,
                estimateTravelSeconds(route, edgeId)
        ));
        monitoringEventService.publishAfterCommit(
                DomainEventType.OHT_MOVED,
                now,
                Map.of(
                        "requestId", request.getRequestId(),
                        "ohtId", oht.getOhtId(),
                        "fromNodeId", fromNodeId,
                        "toNodeId", toNodeId,
                        "currentNodeId", toNodeId,
                        "progressRate", toNodeId.equals(request.getDestinationNodeId()) ? 100 : 50
                )
        );

        if (toNodeId.equals(request.getDestinationNodeId())) {
            completeRequest(request, oht, now);
        }
    }

    private long estimateTravelSeconds(RouteResult route, String edgeId) {
        if (route.pathEdgeIds().size() <= 1) {
            return route.estimatedTravelSeconds();
        }
        return Math.max(1, Math.round(route.estimatedTravelSeconds() / (double) route.pathEdgeIds().size()));
    }

    private void completeRequest(TransferRequest request, Oht oht, OffsetDateTime now) {
        TransferRequestStatus previousStatus = request.getStatus();
        request.complete(now);
        oht.release(now);
        transferHistoryRepository.save(new TransferHistory(
                request.getRequestId(),
                previousStatus,
                request.getStatus(),
                "반송 완료",
                now
        ));
        long elapsedSeconds = request.getRequestedAt() == null
                ? 0
                : java.time.Duration.between(request.getRequestedAt(), now).toSeconds();
        monitoringEventService.publishAfterCommit(
                DomainEventType.TRANSFER_COMPLETED,
                now,
                Map.of(
                        "requestId", request.getRequestId(),
                        "ohtId", oht.getOhtId(),
                        "elapsedSeconds", elapsedSeconds
                )
        );
        if (elapsedSeconds > amhsProperties.analytics().delayThresholdSeconds()) {
            monitoringEventService.publishAfterCommit(
                    DomainEventType.TRANSFER_DELAYED,
                    now,
                    Map.of(
                            "requestId", request.getRequestId(),
                            "ohtId", oht.getOhtId(),
                            "elapsedSeconds", elapsedSeconds,
                            "delayThresholdSeconds", amhsProperties.analytics().delayThresholdSeconds()
                    )
            );
        }
    }

    private void failRequest(TransferRequest request, String reason, OffsetDateTime now) {
        TransferRequestStatus previousStatus = request.getStatus();
        request.fail(reason, now);
        if (request.getAssignedOhtId() != null) {
            ohtRepository.findById(request.getAssignedOhtId()).ifPresent(oht -> {
                if (oht.getStatus() != OhtStatus.ERROR) {
                    oht.release(now);
                }
            });
        }
        transferHistoryRepository.save(new TransferHistory(
                request.getRequestId(),
                previousStatus,
                request.getStatus(),
                reason,
                now
        ));
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("requestId", request.getRequestId());
        if (request.getAssignedOhtId() != null) {
            eventData.put("ohtId", request.getAssignedOhtId());
        }
        eventData.put("failedReason", reason);
        monitoringEventService.publishAfterCommit(DomainEventType.TRANSFER_FAILED, now, eventData);
        if ("OHT_ERROR_OCCURRED".equals(reason)) {
            monitoringEventService.publishAfterCommit(DomainEventType.OHT_ERROR_OCCURRED, now, eventData);
        }
        if ("ROUTE_NOT_FOUND".equals(reason)) {
            monitoringEventService.publishAfterCommit(DomainEventType.ROUTE_NOT_FOUND, now, eventData);
        }
    }
}
