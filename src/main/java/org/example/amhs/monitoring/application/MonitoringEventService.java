package org.example.amhs.monitoring.application;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.amhs.common.config.AmhsProperties;
import org.example.amhs.monitoring.dto.MonitoringEventPayload;
import org.example.amhs.monitoring.event.AlertSeverity;
import org.example.amhs.monitoring.event.DomainEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class MonitoringEventService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringEventService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AmhsProperties properties;

    public MonitoringEventService(AmhsProperties properties) {
        this.properties = properties;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(properties.monitoring().sseTimeoutMs());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        sendHeartbeat(emitter);
        return emitter;
    }

    public void publishAfterCommit(
            DomainEventType eventType,
            OffsetDateTime occurredAt,
            Map<String, Object> data
    ) {
        MonitoringEventPayload payload = createPayload(eventType, occurredAt, data);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(payload);
                }
            });
            return;
        }
        publish(payload);
    }

    public MonitoringEventPayload createPayload(
            DomainEventType eventType,
            OffsetDateTime occurredAt,
            Map<String, Object> data
    ) {
        return new MonitoringEventPayload(
                "evt_" + UUID.randomUUID(),
                eventType,
                occurredAt,
                withAlertFields(eventType, data == null ? Map.of() : data)
        );
    }

    public int connectionCount() {
        return emitters.size();
    }

    private void publish(MonitoringEventPayload payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(payload.eventId())
                        .name(payload.eventType().name())
                        .data(payload));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
                log.info("SSE 연결 전송 실패로 emitter를 정리했습니다. eventType={}", payload.eventType());
            }
        }
    }

    @Scheduled(fixedDelayString = "${amhs.monitoring.heartbeat-interval-ms}")
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            sendHeartbeat(emitter);
        }
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);
        }
    }

    private Map<String, Object> withAlertFields(DomainEventType eventType, Map<String, Object> data) {
        Map<String, Object> enrichedData = new LinkedHashMap<>(data);
        AlertSeverity severity = severityOf(eventType);
        enrichedData.putIfAbsent("alertSeverity", severity.name());
        enrichedData.putIfAbsent("alertTitle", titleOf(eventType));
        enrichedData.putIfAbsent("alertMessage", messageOf(eventType, data));
        return enrichedData;
    }

    private AlertSeverity severityOf(DomainEventType eventType) {
        return switch (eventType) {
            case TRANSFER_FAILED, OHT_ERROR_OCCURRED, ROUTE_NOT_FOUND -> AlertSeverity.CRITICAL;
            case TRANSFER_DELAYED, TRANSFER_CANCELED, EDGE_BLOCKED -> AlertSeverity.WARNING;
            default -> AlertSeverity.INFO;
        };
    }

    private String titleOf(DomainEventType eventType) {
        return switch (eventType) {
            case TRANSFER_CREATED -> "반송 요청 생성";
            case OHT_ASSIGNED -> "OHT 배정";
            case TRANSFER_STARTED -> "반송 시작";
            case OHT_MOVED -> "OHT 이동";
            case TRANSFER_COMPLETED -> "반송 완료";
            case TRANSFER_DELAYED -> "반송 지연";
            case TRANSFER_FAILED -> "반송 실패";
            case TRANSFER_CANCELED -> "반송 취소";
            case OHT_ERROR_OCCURRED -> "OHT 오류";
            case OHT_RECOVERED -> "OHT 복구";
            case EDGE_BLOCKED -> "경로 차단";
            case EDGE_UNBLOCKED -> "경로 차단 해제";
            case ROUTE_NOT_FOUND -> "경로 없음";
        };
    }

    private String messageOf(DomainEventType eventType, Map<String, Object> data) {
        return switch (eventType) {
            case TRANSFER_DELAYED -> "반송 요청 " + valueOf(data, "requestId") + "이 지연 기준을 초과했습니다.";
            case TRANSFER_FAILED -> "반송 요청 " + valueOf(data, "requestId") + "이 실패했습니다.";
            case OHT_ERROR_OCCURRED -> "OHT " + valueOf(data, "ohtId") + "에서 오류가 발생했습니다.";
            case EDGE_BLOCKED -> "FAB 엣지 " + valueOf(data, "edgeId") + "가 차단되었습니다.";
            case ROUTE_NOT_FOUND -> "반송 요청 " + valueOf(data, "requestId") + "의 이동 경로를 찾을 수 없습니다.";
            default -> titleOf(eventType) + " 이벤트가 발생했습니다.";
        };
    }

    private String valueOf(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "-" : value.toString();
    }
}
