package org.example.amhs.analytics.dto;

public record AnalyticsSummaryResponse(
        long totalRequests,
        long completedRequests,
        long failedRequests,
        long canceledRequests,
        double completionRate,
        double failureRate,
        double averageTransferSeconds,
        long p95TransferSeconds,
        long delayedRequests
) {
}
