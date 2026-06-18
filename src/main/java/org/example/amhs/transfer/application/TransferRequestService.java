package org.example.amhs.transfer.application;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.dispatch.application.DispatchService;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.monitoring.application.MonitoringEventService;
import org.example.amhs.monitoring.event.DomainEventType;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.routing.application.RoutingService;
import org.example.amhs.routing.domain.RouteResult;
import org.example.amhs.transfer.domain.TransferHistory;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.dto.AssignTransferRequestRequest;
import org.example.amhs.transfer.dto.AssignTransferRequestResponse;
import org.example.amhs.transfer.dto.CancelTransferRequestRequest;
import org.example.amhs.transfer.dto.CancelTransferRequestResponse;
import org.example.amhs.transfer.dto.CreateTransferRequestRequest;
import org.example.amhs.transfer.dto.StartTransferRequestResponse;
import org.example.amhs.transfer.dto.TransferHistoryResponse;
import org.example.amhs.transfer.dto.TransferRequestDetailResponse;
import org.example.amhs.transfer.dto.TransferRequestResponse;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferRequestService {

    private final TransferRequestRepository transferRequestRepository;
    private final TransferHistoryRepository transferHistoryRepository;
    private final FabNodeRepository fabNodeRepository;
    private final OhtRepository ohtRepository;
    private final DispatchService dispatchService;
    private final RoutingService routingService;
    private final TimeProvider timeProvider;
    private final MonitoringEventService monitoringEventService;

    public TransferRequestService(
            TransferRequestRepository transferRequestRepository,
            TransferHistoryRepository transferHistoryRepository,
            FabNodeRepository fabNodeRepository,
            OhtRepository ohtRepository,
            DispatchService dispatchService,
            RoutingService routingService,
            TimeProvider timeProvider,
            MonitoringEventService monitoringEventService
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.transferHistoryRepository = transferHistoryRepository;
        this.fabNodeRepository = fabNodeRepository;
        this.ohtRepository = ohtRepository;
        this.dispatchService = dispatchService;
        this.routingService = routingService;
        this.timeProvider = timeProvider;
        this.monitoringEventService = monitoringEventService;
    }

    @Transactional
    public TransferRequestResponse create(CreateTransferRequestRequest request) {
        validateNodeExists(request.sourceNodeId());
        validateNodeExists(request.destinationNodeId());

        TransferRequest transferRequest = TransferRequest.create(
                request.sourceNodeId(),
                request.destinationNodeId(),
                request.priority(),
                timeProvider.now()
        );
        TransferRequest saved = transferRequestRepository.save(transferRequest);
        monitoringEventService.publishAfterCommit(
                DomainEventType.TRANSFER_CREATED,
                saved.getRequestedAt(),
                Map.of(
                        "requestId", saved.getRequestId(),
                        "sourceNodeId", saved.getSourceNodeId(),
                        "destinationNodeId", saved.getDestinationNodeId(),
                        "priority", saved.getPriority().name(),
                        "status", saved.getStatus().name()
                )
        );
        return TransferRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<TransferRequestResponse> search(
            TransferRequestStatus status,
            TransferPriority priority,
            String assignedOhtId,
            String sourceNodeId,
            String destinationNodeId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        return transferRequestRepository.findAll(
                filter(status, priority, assignedOhtId, sourceNodeId, destinationNodeId, from, to),
                pageable
        ).map(TransferRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public TransferRequestDetailResponse get(Long requestId) {
        TransferRequest request = getRequest(requestId);
        List<TransferHistoryResponse> histories = transferHistoryRepository
                .findByRequestIdOrderByOccurredAtAsc(requestId).stream()
                .map(TransferHistoryResponse::from)
                .toList();
        List<String> path = calculatePathIfPossible(request);
        return TransferRequestDetailResponse.of(request, path, histories);
    }

    @Transactional
    public AssignTransferRequestResponse assign(Long requestId, AssignTransferRequestRequest request) {
        TransferRequest transferRequest = getRequest(requestId);
        if (transferRequest.getStatus() != TransferRequestStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_STATUS, Map.of(
                    "requestId", requestId,
                    "status", transferRequest.getStatus().name()
            ));
        }

        String ohtId = resolveOhtId(request, transferRequest.getSourceNodeId());
        Oht oht = getOht(ohtId);
        validateAssignableOht(oht);
        RouteResult routeToSource = routingService.findShortestPath(oht.getCurrentNodeId(), transferRequest.getSourceNodeId());

        OffsetDateTime now = timeProvider.now();
        int updatedRows = ohtRepository.reserveIfIdle(ohtId, requestId, now);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.OHT_ALREADY_ASSIGNED, Map.of("ohtId", ohtId));
        }

        TransferRequestStatus previousStatus = transferRequest.getStatus();
        transferRequest.assign(ohtId, now);
        transferHistoryRepository.save(new TransferHistory(
                requestId,
                previousStatus,
                transferRequest.getStatus(),
                "OHT 배정",
                now
        ));
        monitoringEventService.publishAfterCommit(
                DomainEventType.OHT_ASSIGNED,
                now,
                Map.of(
                        "requestId", requestId,
                        "ohtId", ohtId,
                        "status", transferRequest.getStatus().name()
                )
        );

        return new AssignTransferRequestResponse(
                transferRequest.getRequestId(),
                transferRequest.getStatus(),
                transferRequest.getAssignedOhtId(),
                transferRequest.getAssignedAt(),
                routeToSource.pathNodeIds()
        );
    }

    @Transactional
    public CancelTransferRequestResponse cancel(Long requestId, CancelTransferRequestRequest request) {
        TransferRequest transferRequest = getRequest(requestId);
        TransferRequestStatus previousStatus = transferRequest.getStatus();
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "반송 요청 취소"
                : request.reason();
        OffsetDateTime now = timeProvider.now();

        transferRequest.cancel(reason, now);
        if (previousStatus == TransferRequestStatus.ASSIGNED && transferRequest.getAssignedOhtId() != null) {
            Oht oht = getOht(transferRequest.getAssignedOhtId());
            oht.release(now);
        }
        transferHistoryRepository.save(new TransferHistory(
                requestId,
                previousStatus,
                transferRequest.getStatus(),
                reason,
                now
        ));
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("requestId", requestId);
        if (transferRequest.getAssignedOhtId() != null) {
            eventData.put("ohtId", transferRequest.getAssignedOhtId());
        }
        eventData.put("status", transferRequest.getStatus().name());
        eventData.put("reason", reason);
        monitoringEventService.publishAfterCommit(DomainEventType.TRANSFER_CANCELED, now, eventData);

        return new CancelTransferRequestResponse(
                transferRequest.getRequestId(),
                transferRequest.getStatus(),
                reason
        );
    }

    @Transactional
    public StartTransferRequestResponse start(Long requestId) {
        TransferRequest transferRequest = getRequest(requestId);
        if (transferRequest.getStatus() != TransferRequestStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_STATUS, Map.of(
                    "requestId", requestId,
                    "status", transferRequest.getStatus().name()
            ));
        }
        if (transferRequest.getAssignedOhtId() == null) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_STATUS, Map.of("requestId", requestId));
        }

        Oht oht = getOht(transferRequest.getAssignedOhtId());
        RouteResult route = routingService.findShortestPath(
                transferRequest.getSourceNodeId(),
                transferRequest.getDestinationNodeId()
        );
        if (route.pathNodeIds().isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND, Map.of("requestId", requestId));
        }

        OffsetDateTime now = timeProvider.now();
        TransferRequestStatus previousStatus = transferRequest.getStatus();
        transferRequest.startMoving(now);
        oht.startMoving(now);
        transferHistoryRepository.save(new TransferHistory(
                requestId,
                previousStatus,
                transferRequest.getStatus(),
                "반송 시작",
                now
        ));
        monitoringEventService.publishAfterCommit(
                DomainEventType.TRANSFER_STARTED,
                now,
                Map.of(
                        "requestId", requestId,
                        "ohtId", transferRequest.getAssignedOhtId(),
                        "status", transferRequest.getStatus().name()
                )
        );

        return new StartTransferRequestResponse(
                transferRequest.getRequestId(),
                transferRequest.getStatus(),
                transferRequest.getAssignedOhtId(),
                transferRequest.getStartedAt()
        );
    }

    private String resolveOhtId(AssignTransferRequestRequest request, String sourceNodeId) {
        if (request != null && request.ohtId() != null && !request.ohtId().isBlank()) {
            return request.ohtId();
        }
        return dispatchService.selectBestOht(sourceNodeId).ohtId();
    }

    private void validateAssignableOht(Oht oht) {
        if (oht.getStatus() != OhtStatus.IDLE || oht.getCurrentRequestId() != null) {
            throw new BusinessException(ErrorCode.INVALID_OHT_STATUS, Map.of(
                    "ohtId", oht.getOhtId(),
                    "status", oht.getStatus().name()
            ));
        }
    }

    private List<String> calculatePathIfPossible(TransferRequest request) {
        try {
            return routingService.findShortestPath(
                    request.getSourceNodeId(),
                    request.getDestinationNodeId()
            ).pathNodeIds();
        } catch (BusinessException exception) {
            return List.of();
        }
    }

    private TransferRequest getRequest(Long requestId) {
        return transferRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TRANSFER_REQUEST_NOT_FOUND,
                        Map.of("requestId", requestId)
                ));
    }

    private Oht getOht(String ohtId) {
        return ohtRepository.findById(ohtId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OHT_NOT_FOUND, Map.of("ohtId", ohtId)));
    }

    private void validateNodeExists(String nodeId) {
        if (!fabNodeRepository.existsById(nodeId)) {
            throw new ResourceNotFoundException(ErrorCode.FAB_NODE_NOT_FOUND, Map.of("nodeId", nodeId));
        }
    }

    private Specification<TransferRequest> filter(
            TransferRequestStatus status,
            TransferPriority priority,
            String assignedOhtId,
            String sourceNodeId,
            String destinationNodeId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }
            if (assignedOhtId != null && !assignedOhtId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("assignedOhtId"), assignedOhtId));
            }
            if (sourceNodeId != null && !sourceNodeId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("sourceNodeId"), sourceNodeId));
            }
            if (destinationNodeId != null && !destinationNodeId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("destinationNodeId"), destinationNodeId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("requestedAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("requestedAt"), to));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
