package org.example.amhs.analytics.dto;

public record OhtThroughputResponse(
        String ohtId,
        long completedRequests,
        long failedRequests,
        double averageTransferSeconds
) {
}
