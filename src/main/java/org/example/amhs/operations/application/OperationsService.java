package org.example.amhs.operations.application;

import java.util.Comparator;
import java.util.List;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.operations.dto.OperationsBlockedEdgeResponse;
import org.example.amhs.operations.dto.OperationsOhtIssueResponse;
import org.example.amhs.operations.dto.OperationsOverviewResponse;
import org.example.amhs.operations.dto.OperationsProblemTransferResponse;
import org.example.amhs.operations.dto.OperationsStatusCountResponse;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository.TransferStatusSummary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsService {

    private final TransferRequestRepository transferRequestRepository;
    private final OhtRepository ohtRepository;
    private final FabEdgeRepository fabEdgeRepository;

    public OperationsService(
            TransferRequestRepository transferRequestRepository,
            OhtRepository ohtRepository,
            FabEdgeRepository fabEdgeRepository
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.ohtRepository = ohtRepository;
        this.fabEdgeRepository = fabEdgeRepository;
    }

    @Transactional(readOnly = true)
    public OperationsOverviewResponse getOverview(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<Oht> ohts = ohtRepository.findAll();
        List<FabEdge> edges = fabEdgeRepository.findAll();

        List<OperationsProblemTransferResponse> recentProblemTransfers = transferRequestRepository.findByStatusIn(
                        List.of(TransferRequestStatus.FAILED, TransferRequestStatus.CANCELED),
                        PageRequest.of(0, normalizedLimit, Sort.by(
                                Sort.Order.desc("completedAt"),
                                Sort.Order.desc("requestId")
                        ))
                )
                .stream()
                .map(OperationsProblemTransferResponse::from)
                .toList();

        List<OperationsOhtIssueResponse> abnormalOhts = ohts.stream()
                .filter(oht -> oht.getStatus() == OhtStatus.ERROR)
                .sorted(Comparator.comparing(Oht::getOhtId))
                .map(OperationsOhtIssueResponse::from)
                .toList();

        List<OperationsBlockedEdgeResponse> blockedEdges = edges.stream()
                .filter(FabEdge::isBlocked)
                .sorted(Comparator.comparing(FabEdge::getEdgeId))
                .map(OperationsBlockedEdgeResponse::from)
                .toList();

        return new OperationsOverviewResponse(
                toCounts(),
                recentProblemTransfers,
                abnormalOhts,
                blockedEdges
        );
    }

    private OperationsStatusCountResponse toCounts() {
        TransferStatusSummary transferCounts = transferRequestRepository.summarizeStatusCounts();
        return new OperationsStatusCountResponse(
                numberOrZero(transferCounts.getWaitingTransfers()),
                numberOrZero(transferCounts.getAssignedTransfers()),
                numberOrZero(transferCounts.getMovingTransfers()),
                numberOrZero(transferCounts.getCompletedTransfers()),
                numberOrZero(transferCounts.getFailedTransfers()),
                numberOrZero(transferCounts.getCanceledTransfers()),
                ohtRepository.countByStatus(OhtStatus.IDLE),
                ohtRepository.countByStatus(OhtStatus.RESERVED),
                ohtRepository.countByStatus(OhtStatus.MOVING),
                ohtRepository.countByStatus(OhtStatus.ERROR),
                fabEdgeRepository.countByBlocked(true)
        );
    }

    private long numberOrZero(Number value) {
        if (value == null) {
            return 0;
        }
        return value.longValue();
    }
}
