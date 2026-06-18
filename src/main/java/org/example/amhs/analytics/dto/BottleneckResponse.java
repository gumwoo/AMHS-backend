package org.example.amhs.analytics.dto;

public record BottleneckResponse(
        String edgeId,
        String fromNodeId,
        String toNodeId,
        long passCount,
        double averageTravelSeconds,
        long p95TravelSeconds,
        long delayedCount
) {
}
