package org.example.amhs.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DemoMonitoringServiceTest {

    @Autowired
    private DemoMonitoringService demoMonitoringService;

    @AfterEach
    void tearDown() {
        demoMonitoringService.stop();
    }

    @Test
    void 데모_모니터링을_시작하고_이벤트를_발행한다() {
        var started = demoMonitoringService.start();
        long before = started.emittedEvents();

        var tick = demoMonitoringService.tickOnce();
        var status = demoMonitoringService.status();

        assertThat(started.running()).isTrue();
        assertThat(tick.message()).isEqualTo("데모 이벤트를 발행했습니다.");
        assertThat(status.emittedEvents()).isGreaterThan(before);
        assertThat(status.lastEventAt()).isNotNull();
    }
}
