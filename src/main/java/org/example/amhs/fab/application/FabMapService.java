package org.example.amhs.fab.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.dto.FabEdgeResponse;
import org.example.amhs.fab.dto.FabEdgeStatusResponse;
import org.example.amhs.fab.dto.FabMapResponse;
import org.example.amhs.fab.dto.FabNodeResponse;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.monitoring.application.MonitoringEventService;
import org.example.amhs.monitoring.event.DomainEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class FabMapService {

    private final FabNodeRepository fabNodeRepository;
    private final FabEdgeRepository fabEdgeRepository;
    private final MonitoringEventService monitoringEventService;

    public FabMapService(
            FabNodeRepository fabNodeRepository,
            FabEdgeRepository fabEdgeRepository,
            MonitoringEventService monitoringEventService
    ) {
        this.fabNodeRepository = fabNodeRepository;
        this.fabEdgeRepository = fabEdgeRepository;
        this.monitoringEventService = monitoringEventService;
    }

    @Transactional(readOnly = true)
    public FabMapResponse getFabMap() {
        var nodes = fabNodeRepository.findAll().stream()
                .sorted(Comparator.comparing(node -> node.getNodeId()))
                .map(FabNodeResponse::from)
                .toList();
        var edges = fabEdgeRepository.findAll().stream()
                .sorted(Comparator.comparing(edge -> edge.getEdgeId()))
                .map(FabEdgeResponse::from)
                .toList();
        return new FabMapResponse(nodes, edges);
    }

    @Transactional
    public FabEdgeStatusResponse blockEdge(String edgeId, String reason) {
        FabEdge edge = getEdge(edgeId);
        edge.block();
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("edgeId", edge.getEdgeId());
        eventData.put("fromNodeId", edge.getFromNodeId());
        eventData.put("toNodeId", edge.getToNodeId());
        if (reason != null) {
            eventData.put("reason", reason);
        }
        monitoringEventService.publishAfterCommit(
                DomainEventType.EDGE_BLOCKED,
                OffsetDateTime.now(),
                eventData
        );
        return new FabEdgeStatusResponse(edge.getEdgeId(), edge.isBlocked(), reason);
    }

    @Transactional
    public FabEdgeStatusResponse unblockEdge(String edgeId) {
        FabEdge edge = getEdge(edgeId);
        edge.unblock();
        monitoringEventService.publishAfterCommit(
                DomainEventType.EDGE_UNBLOCKED,
                OffsetDateTime.now(),
                Map.of(
                        "edgeId", edge.getEdgeId(),
                        "fromNodeId", edge.getFromNodeId(),
                        "toNodeId", edge.getToNodeId()
                )
        );
        return new FabEdgeStatusResponse(edge.getEdgeId(), edge.isBlocked(), null);
    }

    private FabEdge getEdge(String edgeId) {
        return fabEdgeRepository.findById(edgeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FAB_EDGE_NOT_FOUND,
                        Map.of("edgeId", edgeId)
                ));
    }
}
