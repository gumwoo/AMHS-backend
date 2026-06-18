package org.example.amhs.monitoring.application;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.amhs.common.config.AmhsProperties;
import org.example.amhs.monitoring.dto.MonitoringEventPayload;
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
                data == null ? Map.of() : data
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
}
