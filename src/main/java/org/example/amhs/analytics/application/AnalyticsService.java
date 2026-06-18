package org.example.amhs.analytics.application;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.example.amhs.analytics.dto.AnalyticsSummaryResponse;
import org.example.amhs.analytics.dto.BottleneckResponse;
import org.example.amhs.analytics.dto.OhtThroughputResponse;
import org.example.amhs.common.config.AmhsProperties;
import org.example.amhs.oht.domain.OhtMoveEvent;
import org.example.amhs.oht.repository.OhtMoveEventRepository;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final TransferRequestRepository transferRequestRepository;
    private final OhtMoveEventRepository ohtMoveEventRepository;
    private final AmhsProperties amhsProperties;

    public AnalyticsService(
            TransferRequestRepository transferRequestRepository,
            OhtMoveEventRepository ohtMoveEventRepository,
            AmhsProperties amhsProperties
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.ohtMoveEventRepository = ohtMoveEventRepository;
        this.amhsProperties = amhsProperties;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(OffsetDateTime from, OffsetDateTime to) {
        List<TransferRequest> requests = transferRequestRepository.findAll().stream()
                .filter(request -> isInRange(request.getRequestedAt(), from, to))
                .toList();
        List<Long> completedDurations = requests.stream()
                .filter(request -> request.getStatus() == TransferRequestStatus.COMPLETED)
                .map(this::transferSeconds)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        long totalRequests = requests.size();
        long completedRequests = countByStatus(requests, TransferRequestStatus.COMPLETED);
        long failedRequests = countByStatus(requests, TransferRequestStatus.FAILED);
        long canceledRequests = countByStatus(requests, TransferRequestStatus.CANCELED);
        long delayedRequests = completedDurations.stream()
                .filter(seconds -> seconds > amhsProperties.analytics().delayThresholdSeconds())
                .count();

        return new AnalyticsSummaryResponse(
                totalRequests,
                completedRequests,
                failedRequests,
                canceledRequests,
                rate(completedRequests, totalRequests),
                rate(failedRequests, totalRequests),
                average(completedDurations),
                percentile95(completedDurations),
                delayedRequests
        );
    }

    @Transactional(readOnly = true)
    public List<OhtThroughputResponse> getOhtThroughput(OffsetDateTime from, OffsetDateTime to) {
        return transferRequestRepository.findAll().stream()
                .filter(request -> isInRange(request.getRequestedAt(), from, to))
                .filter(request -> request.getAssignedOhtId() != null)
                .filter(request -> request.getStatus() == TransferRequestStatus.COMPLETED
                        || request.getStatus() == TransferRequestStatus.FAILED)
                .collect(Collectors.groupingBy(TransferRequest::getAssignedOhtId))
                .entrySet()
                .stream()
                .map(entry -> toThroughput(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(OhtThroughputResponse::completedRequests).reversed()
                        .thenComparing(OhtThroughputResponse::ohtId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BottleneckResponse> getBottlenecks(OffsetDateTime from, OffsetDateTime to, int limit) {
        return ohtMoveEventRepository.findAll().stream()
                .filter(event -> isInRange(event.getOccurredAt(), from, to))
                .collect(Collectors.groupingBy(BottleneckKey::from))
                .entrySet()
                .stream()
                .map(entry -> toBottleneck(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BottleneckResponse::averageTravelSeconds).reversed()
                        .thenComparing(BottleneckResponse::passCount, Comparator.reverseOrder())
                        .thenComparing(BottleneckResponse::edgeId))
                .limit(Math.max(1, limit))
                .toList();
    }

    private OhtThroughputResponse toThroughput(String ohtId, List<TransferRequest> requests) {
        long completedRequests = countByStatus(requests, TransferRequestStatus.COMPLETED);
        long failedRequests = countByStatus(requests, TransferRequestStatus.FAILED);
        List<Long> completedDurations = requests.stream()
                .filter(request -> request.getStatus() == TransferRequestStatus.COMPLETED)
                .map(this::transferSeconds)
                .filter(Objects::nonNull)
                .toList();
        return new OhtThroughputResponse(ohtId, completedRequests, failedRequests, average(completedDurations));
    }

    private BottleneckResponse toBottleneck(BottleneckKey key, List<OhtMoveEvent> events) {
        List<Long> travelSeconds = events.stream()
                .map(OhtMoveEvent::getTravelSeconds)
                .sorted()
                .toList();
        long delayedCount = travelSeconds.stream()
                .filter(seconds -> seconds > amhsProperties.analytics().delayThresholdSeconds())
                .count();
        return new BottleneckResponse(
                key.edgeId(),
                key.fromNodeId(),
                key.toNodeId(),
                events.size(),
                average(travelSeconds),
                percentile95(travelSeconds),
                delayedCount
        );
    }

    private long countByStatus(List<TransferRequest> requests, TransferRequestStatus status) {
        return requests.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    private Long transferSeconds(TransferRequest request) {
        if (request.getRequestedAt() == null || request.getCompletedAt() == null) {
            return null;
        }
        return Duration.between(request.getRequestedAt(), request.getCompletedAt()).toSeconds();
    }

    private boolean isInRange(OffsetDateTime occurredAt, OffsetDateTime from, OffsetDateTime to) {
        if (occurredAt == null) {
            return false;
        }
        return (from == null || !occurredAt.isBefore(from))
                && (to == null || !occurredAt.isAfter(to));
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return numerator / (double) denominator;
    }

    private double average(List<Long> values) {
        return values.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }

    private long percentile95(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(sortedValues.size() * 0.95) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private record BottleneckKey(String edgeId, String fromNodeId, String toNodeId) {

        private static BottleneckKey from(OhtMoveEvent event) {
            return new BottleneckKey(event.getEdgeId(), event.getFromNodeId(), event.getToNodeId());
        }
    }
}
