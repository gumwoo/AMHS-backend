package org.example.amhs.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.example.amhs.oht.domain.OhtMoveEvent;
import org.example.amhs.oht.repository.OhtMoveEventRepository;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class AnalyticsServiceTest {

    private final OffsetDateTime baseTime = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Autowired
    private OhtMoveEventRepository ohtMoveEventRepository;

    @BeforeEach
    void setUp() {
        ohtMoveEventRepository.deleteAll();
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
    }

    @Test
    void 반송_요청_요약_지표를_집계한다() {
        saveCompletedRequest("OHT-01", 80);
        saveCompletedRequest("OHT-02", 180);
        saveFailedRequest("OHT-01");
        saveCanceledRequest();

        var response = analyticsService.getSummary(baseTime.minusMinutes(1), baseTime.plusMinutes(10));

        assertThat(response.totalRequests()).isEqualTo(4);
        assertThat(response.completedRequests()).isEqualTo(2);
        assertThat(response.failedRequests()).isEqualTo(1);
        assertThat(response.canceledRequests()).isEqualTo(1);
        assertThat(response.completionRate()).isEqualTo(0.5);
        assertThat(response.failureRate()).isEqualTo(0.25);
        assertThat(response.averageTransferSeconds()).isEqualTo(130);
        assertThat(response.p95TransferSeconds()).isEqualTo(180);
        assertThat(response.delayedRequests()).isEqualTo(1);
    }

    @Test
    void OHT별_처리량을_집계한다() {
        saveCompletedRequest("OHT-01", 80);
        saveCompletedRequest("OHT-01", 100);
        saveFailedRequest("OHT-01");
        saveCompletedRequest("OHT-02", 60);
        saveCompletedRequest("OHT-03", baseTime.plusDays(2), 40);

        var responses = analyticsService.getOhtThroughput(baseTime.minusMinutes(1), baseTime.plusMinutes(10));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).ohtId()).isEqualTo("OHT-01");
        assertThat(responses.get(0).completedRequests()).isEqualTo(2);
        assertThat(responses.get(0).failedRequests()).isEqualTo(1);
        assertThat(responses.get(0).averageTransferSeconds()).isEqualTo(90);
    }

    @Test
    void 이동_이벤트로_병목_구간을_집계한다() {
        ohtMoveEventRepository.save(new OhtMoveEvent(
                "evt-001", "OHT-01", 1L, "A", "B", "EDGE-001", baseTime, 30
        ));
        ohtMoveEventRepository.save(new OhtMoveEvent(
                "evt-002", "OHT-02", 2L, "A", "B", "EDGE-001", baseTime.plusSeconds(1), 150
        ));
        ohtMoveEventRepository.save(new OhtMoveEvent(
                "evt-003", "OHT-03", 3L, "B", "C", "EDGE-002", baseTime.plusSeconds(2), 90
        ));
        ohtMoveEventRepository.save(new OhtMoveEvent(
                "evt-004", "OHT-04", 4L, "C", "D", "EDGE-003", baseTime.plusDays(2), 300
        ));

        var responses = analyticsService.getBottlenecks(baseTime.minusMinutes(1), baseTime.plusMinutes(10), 10);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).edgeId()).isEqualTo("EDGE-001");
        assertThat(responses.get(0).passCount()).isEqualTo(2);
        assertThat(responses.get(0).averageTravelSeconds()).isEqualTo(90);
        assertThat(responses.get(0).p95TravelSeconds()).isEqualTo(150);
        assertThat(responses.get(0).delayedCount()).isEqualTo(1);
    }

    private void saveCompletedRequest(String ohtId, long elapsedSeconds) {
        saveCompletedRequest(ohtId, baseTime, elapsedSeconds);
    }

    private void saveCompletedRequest(String ohtId, OffsetDateTime requestedAt, long elapsedSeconds) {
        TransferRequest request = TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL,
                requestedAt
        );
        request.assign(ohtId, requestedAt.plusSeconds(1));
        request.startMoving(requestedAt.plusSeconds(2));
        request.complete(requestedAt.plusSeconds(elapsedSeconds));
        transferRequestRepository.save(request);
    }

    private void saveFailedRequest(String ohtId) {
        TransferRequest request = TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL,
                baseTime.plusMinutes(1)
        );
        request.assign(ohtId, baseTime.plusMinutes(1).plusSeconds(1));
        request.fail("테스트 실패", baseTime.plusMinutes(1).plusSeconds(30));
        transferRequestRepository.save(request);
    }

    private void saveCanceledRequest() {
        TransferRequest request = TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL,
                baseTime.plusMinutes(2)
        );
        request.cancel("테스트 취소", baseTime.plusMinutes(2).plusSeconds(10));
        transferRequestRepository.save(request);
    }
}
