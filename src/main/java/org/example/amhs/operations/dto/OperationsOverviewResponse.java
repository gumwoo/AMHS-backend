package org.example.amhs.operations.dto;

import java.util.List;

public record OperationsOverviewResponse(
        OperationsStatusCountResponse counts,
        List<OperationsProblemTransferResponse> recentProblemTransfers,
        List<OperationsOhtIssueResponse> abnormalOhts,
        List<OperationsBlockedEdgeResponse> blockedEdges
) {
}
