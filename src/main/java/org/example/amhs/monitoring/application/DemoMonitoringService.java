package org.example.amhs.monitoring.application;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.example.amhs.common.config.AmhsProperties;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.monitoring.dto.DemoMonitoringActionResponse;
import org.example.amhs.monitoring.dto.DemoMonitoringStatusResponse;
import org.example.amhs.monitoring.event.DomainEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DemoMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(DemoMonitoringService.class);
    private static final List<String> OHT_IDS = List.of("OHT-01", "OHT-02", "OHT-03", "OHT-04");
    private static final List<String> NODE_IDS = List.of(
            "STOCKER-A", "STOCKER-B", "EQP-01", "EQP-02", "JUNCTION-01", "JUNCTION-02", "CHARGER-01"
    );
    private static final List<DemoEdge> EDGES = List.of(
            new DemoEdge("EDGE-001", "STOCKER-A", "JUNCTION-01"),
            new DemoEdge("EDGE-002", "STOCKER-B", "JUNCTION-01"),
            new DemoEdge("EDGE-003", "JUNCTION-01", "JUNCTION-02"),
            new DemoEdge("EDGE-004", "JUNCTION-02", "EQP-01"),
            new DemoEdge("EDGE-005", "JUNCTION-02", "EQP-02"),
            new DemoEdge("EDGE-006", "CHARGER-01", "JUNCTION-01")
    );

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong emittedEvents = new AtomicLong(0);
    private final AtomicLong requestSequence = new AtomicLong(1000);

    private final MonitoringEventService monitoringEventService;
    private final TimeProvider timeProvider;
    private final AmhsProperties properties;

    private OffsetDateTime startedAt;
    private OffsetDateTime lastEventAt;

    public DemoMonitoringService(
            MonitoringEventService monitoringEventService,
            TimeProvider timeProvider,
            AmhsProperties properties
    ) {
        this.monitoringEventService = monitoringEventService;
        this.timeProvider = timeProvider;
        this.properties = properties;
    }

    public DemoMonitoringStatusResponse start() {
        if (running.compareAndSet(false, true)) {
            startedAt = timeProvider.now();
            log.info("데모 모니터링 SSE 발생기를 시작했습니다.");
        }
        return status();
    }

    public DemoMonitoringStatusResponse stop() {
        if (running.compareAndSet(true, false)) {
            log.info("데모 모니터링 SSE 발생기를 중지했습니다.");
        }
        return status();
    }

    public DemoMonitoringStatusResponse status() {
        return new DemoMonitoringStatusResponse(
                running.get(),
                startedAt,
                lastEventAt,
                emittedEvents.get(),
                properties.demo().tickIntervalMs(),
                monitoringEventService.connectionCount()
        );
    }

    @Scheduled(fixedDelayString = "${amhs.demo.tick-interval-ms:1500}")
    public void scheduledTick() {
        if (running.get()) {
            tickOnce();
        }
    }

    public DemoMonitoringActionResponse tickOnce() {
        OffsetDateTime now = timeProvider.now();
        DomainEventType eventType = randomEventType();
        Map<String, Object> data = createEventData(eventType);
        data.put("demo", true);

        monitoringEventService.publishAfterCommit(eventType, now, data);
        lastEventAt = now;
        long total = emittedEvents.incrementAndGet();
        return new DemoMonitoringActionResponse(running.get(), "데모 이벤트를 발행했습니다.", now, total);
    }

    private DomainEventType randomEventType() {
        int value = ThreadLocalRandom.current().nextInt(100);
        if (value < 34) {
            return DomainEventType.OHT_MOVED;
        }
        if (value < 48) {
            return DomainEventType.OHT_ASSIGNED;
        }
        if (value < 62) {
            return DomainEventType.TRANSFER_COMPLETED;
        }
        if (value < 72) {
            return DomainEventType.EDGE_BLOCKED;
        }
        if (value < 80) {
            return DomainEventType.EDGE_UNBLOCKED;
        }
        if (value < 88) {
            return DomainEventType.TRANSFER_DELAYED;
        }
        if (value < 94) {
            return DomainEventType.OHT_ERROR_OCCURRED;
        }
        if (value < 98) {
            return DomainEventType.OHT_RECOVERED;
        }
        return DomainEventType.ROUTE_NOT_FOUND;
    }

    private Map<String, Object> createEventData(DomainEventType eventType) {
        Map<String, Object> data = new LinkedHashMap<>();
        long requestId = requestSequence.incrementAndGet();
        String ohtId = pick(OHT_IDS);
        String fromNodeId = pick(NODE_IDS);
        String toNodeId = pickDifferent(NODE_IDS, fromNodeId);
        DemoEdge edge = pick(EDGES);

        switch (eventType) {
            case OHT_MOVED -> {
                data.put("requestId", requestId);
                data.put("ohtId", ohtId);
                data.put("edgeId", edge.edgeId());
                data.put("fromNodeId", edge.fromNodeId());
                data.put("toNodeId", edge.toNodeId());
                data.put("currentNodeId", edge.toNodeId());
                data.put("progressRate", ThreadLocalRandom.current().nextInt(12, 96));
            }
            case OHT_ASSIGNED -> {
                data.put("requestId", requestId);
                data.put("ohtId", ohtId);
                data.put("currentNodeId", fromNodeId);
                data.put("status", "ASSIGNED");
            }
            case TRANSFER_COMPLETED -> {
                data.put("requestId", requestId);
                data.put("ohtId", ohtId);
                data.put("elapsedSeconds", ThreadLocalRandom.current().nextInt(38, 220));
            }
            case TRANSFER_DELAYED -> {
                data.put("requestId", requestId);
                data.put("ohtId", ohtId);
                data.put("elapsedSeconds", ThreadLocalRandom.current().nextInt(160, 420));
                data.put("delayThresholdSeconds", 120);
            }
            case OHT_ERROR_OCCURRED, OHT_RECOVERED -> {
                data.put("ohtId", ohtId);
                data.put("currentNodeId", fromNodeId);
                if (eventType == DomainEventType.OHT_ERROR_OCCURRED) {
                    data.put("errorCode", "E-DEMO-" + ThreadLocalRandom.current().nextInt(100, 999));
                }
            }
            case EDGE_BLOCKED, EDGE_UNBLOCKED -> {
                data.put("edgeId", edge.edgeId());
                data.put("fromNodeId", edge.fromNodeId());
                data.put("toNodeId", edge.toNodeId());
                data.put("reason", eventType == DomainEventType.EDGE_BLOCKED ? "데모 병목 감지" : "데모 차단 해제");
            }
            case ROUTE_NOT_FOUND -> {
                data.put("requestId", requestId);
                data.put("sourceNodeId", fromNodeId);
                data.put("destinationNodeId", toNodeId);
            }
            default -> data.put("requestId", requestId);
        }
        return data;
    }

    private <T> T pick(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private String pickDifferent(List<String> values, String excluded) {
        String value = pick(values);
        while (value.equals(excluded)) {
            value = pick(values);
        }
        return value;
    }

    private record DemoEdge(
            String edgeId,
            String fromNodeId,
            String toNodeId
    ) {
    }
}
