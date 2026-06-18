package org.example.amhs.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;
import org.example.amhs.monitoring.dto.MonitoringEventPayload;
import org.example.amhs.monitoring.event.DomainEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class MonitoringEventServiceTest {

    @Autowired
    private MonitoringEventService monitoringEventService;

    @Test
    void SSE_payload는_계약_필드와_이벤트_상세_필드를_가진다() {
        MonitoringEventPayload payload = monitoringEventService.createPayload(
                DomainEventType.OHT_MOVED,
                OffsetDateTime.parse("2026-06-18T15:30:00+09:00"),
                Map.of(
                        "requestId", 1001L,
                        "ohtId", "OHT-01",
                        "currentNodeId", "EQP-01"
                )
        );

        assertThat(payload.eventId()).startsWith("evt_");
        assertThat(payload.eventType()).isEqualTo(DomainEventType.OHT_MOVED);
        assertThat(payload.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-06-18T15:30:00+09:00"));
        assertThat(payload.data())
                .containsEntry("requestId", 1001L)
                .containsEntry("ohtId", "OHT-01")
                .containsEntry("currentNodeId", "EQP-01")
                .containsEntry("alertSeverity", "INFO")
                .containsEntry("alertTitle", "OHT 이동");
    }

    @Test
    void 위험_이벤트에는_알림_심각도와_메시지를_붙인다() {
        MonitoringEventPayload payload = monitoringEventService.createPayload(
                DomainEventType.ROUTE_NOT_FOUND,
                OffsetDateTime.parse("2026-06-18T15:30:00+09:00"),
                Map.of("requestId", 1001L)
        );

        assertThat(payload.data())
                .containsEntry("alertSeverity", "CRITICAL")
                .containsEntry("alertTitle", "경로 없음")
                .containsEntry("alertMessage", "반송 요청 1001의 이동 경로를 찾을 수 없습니다.");
    }

    @Test
    void 지연_이벤트에는_경고_알림을_붙인다() {
        MonitoringEventPayload payload = monitoringEventService.createPayload(
                DomainEventType.TRANSFER_DELAYED,
                OffsetDateTime.parse("2026-06-18T15:30:00+09:00"),
                Map.of(
                        "requestId", 1001L,
                        "elapsedSeconds", 180L,
                        "delayThresholdSeconds", 120L
                )
        );

        assertThat(payload.data())
                .containsEntry("alertSeverity", "WARNING")
                .containsEntry("alertTitle", "반송 지연")
                .containsEntry("alertMessage", "반송 요청 1001이 지연 기준을 초과했습니다.");
    }
}
