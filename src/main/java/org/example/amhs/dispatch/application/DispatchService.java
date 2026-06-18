package org.example.amhs.dispatch.application;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.common.exception.RouteNotFoundException;
import org.example.amhs.dispatch.dto.DispatchCandidateResponse;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.routing.application.RoutingService;
import org.example.amhs.routing.domain.RouteResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchService {

    private final OhtRepository ohtRepository;
    private final FabNodeRepository fabNodeRepository;
    private final RoutingService routingService;

    public DispatchService(
            OhtRepository ohtRepository,
            FabNodeRepository fabNodeRepository,
            RoutingService routingService
    ) {
        this.ohtRepository = ohtRepository;
        this.fabNodeRepository = fabNodeRepository;
        this.routingService = routingService;
    }

    @Transactional(readOnly = true)
    public DispatchCandidateResponse selectBestOht(String sourceNodeId) {
        if (!fabNodeRepository.existsById(sourceNodeId)) {
            throw new ResourceNotFoundException(ErrorCode.FAB_NODE_NOT_FOUND, Map.of("nodeId", sourceNodeId));
        }

        return ohtRepository.findByStatusAndCurrentRequestIdIsNull(OhtStatus.IDLE).stream()
                .map(oht -> toCandidate(oht, sourceNodeId))
                .filter(candidate -> candidate != null)
                .min(Comparator
                        .comparingDouble(Candidate::distanceToSourceMeters)
                        .thenComparing(Candidate::lastMovedAt)
                        .thenComparing(Candidate::ohtId))
                .map(Candidate::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AVAILABLE_OHT_NOT_FOUND,
                        Map.of("sourceNodeId", sourceNodeId)
                ));
    }

    private Candidate toCandidate(Oht oht, String sourceNodeId) {
        try {
            RouteResult route = routingService.findShortestPath(oht.getCurrentNodeId(), sourceNodeId);
            return new Candidate(
                    oht.getOhtId(),
                    oht.getCurrentNodeId(),
                    route.totalDistanceMeters(),
                    route.estimatedTravelSeconds(),
                    oht.getLastMovedAt()
            );
        } catch (RouteNotFoundException | ResourceNotFoundException exception) {
            return null;
        }
    }

    private record Candidate(
            String ohtId,
            String currentNodeId,
            double distanceToSourceMeters,
            long estimatedTravelSeconds,
            OffsetDateTime lastMovedAt
    ) {

        DispatchCandidateResponse toResponse() {
            return new DispatchCandidateResponse(
                    ohtId,
                    currentNodeId,
                    distanceToSourceMeters,
                    estimatedTravelSeconds
            );
        }
    }
}
