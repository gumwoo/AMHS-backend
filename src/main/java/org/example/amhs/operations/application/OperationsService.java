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
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.repository.TransferRequestRepository;
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
        List<TransferRequest> transfers = transferRequestRepository.findAll();
        List<Oht> ohts = ohtRepository.findAll();
        List<FabEdge> edges = fabEdgeRepository.findAll();

        List<OperationsProblemTransferResponse> recentProblemTransfers = transfers.stream()
                .filter(request -> request.getStatus() == TransferRequestStatus.FAILED
                        || request.getStatus() == TransferRequestStatus.CANCELED)
                .sorted(Comparator.comparing(
                        TransferRequest::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(normalizedLimit)
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
                toCounts(transfers, ohts, edges),
                recentProblemTransfers,
                abnormalOhts,
                blockedEdges
        );
    }

    private OperationsStatusCountResponse toCounts(
            List<TransferRequest> transfers,
            List<Oht> ohts,
            List<FabEdge> edges
    ) {
        return new OperationsStatusCountResponse(
                countTransfers(transfers, TransferRequestStatus.WAITING),
                countTransfers(transfers, TransferRequestStatus.ASSIGNED),
                countTransfers(transfers, TransferRequestStatus.MOVING),
                countTransfers(transfers, TransferRequestStatus.COMPLETED),
                countTransfers(transfers, TransferRequestStatus.FAILED),
                countTransfers(transfers, TransferRequestStatus.CANCELED),
                countOhts(ohts, OhtStatus.IDLE),
                countOhts(ohts, OhtStatus.RESERVED),
                countOhts(ohts, OhtStatus.MOVING),
                countOhts(ohts, OhtStatus.ERROR),
                edges.stream().filter(FabEdge::isBlocked).count()
        );
    }

    private long countTransfers(List<TransferRequest> transfers, TransferRequestStatus status) {
        return transfers.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    private long countOhts(List<Oht> ohts, OhtStatus status) {
        return ohts.stream()
                .filter(oht -> oht.getStatus() == status)
                .count();
    }
}
