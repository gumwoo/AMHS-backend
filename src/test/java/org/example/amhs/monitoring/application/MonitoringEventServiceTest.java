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
                .containsEntry("currentNodeId", "EQP-01");
    }
}
