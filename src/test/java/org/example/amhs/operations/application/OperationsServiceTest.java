package org.example.amhs.operations.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.repository.OhtRepository;
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
class OperationsServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private OperationsService operationsService;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Autowired
    private OhtRepository ohtRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
    }

    @Test
    void 운영_현황을_한번에_조회한다() {
        saveWaitingRequest();
        saveFailedRequest("OHT-01", "경로 없음", now.plusMinutes(3));
        saveCanceledRequest("운영자 취소", now.plusMinutes(2));

        Oht errorOht = Oht.create("OHT-01", "NODE-A", now.minusMinutes(5));
        errorOht.markError(now.minusMinutes(1));
        ohtRepository.save(errorOht);
        ohtRepository.save(Oht.create("OHT-02", "NODE-B", now.minusMinutes(2)));

        FabEdge blockedEdge = new FabEdge("EDGE-001", "NODE-A", "NODE-B", 10, 10, false);
        blockedEdge.block();
        fabEdgeRepository.save(blockedEdge);
        fabEdgeRepository.save(new FabEdge("EDGE-002", "NODE-B", "NODE-C", 10, 10, false));

        var response = operationsService.getOverview(10);

        assertThat(response.counts().waitingTransfers()).isEqualTo(1);
        assertThat(response.counts().failedTransfers()).isEqualTo(1);
        assertThat(response.counts().canceledTransfers()).isEqualTo(1);
        assertThat(response.counts().idleOhts()).isEqualTo(1);
        assertThat(response.counts().errorOhts()).isEqualTo(1);
        assertThat(response.counts().blockedEdges()).isEqualTo(1);
        assertThat(response.abnormalOhts()).extracting("ohtId").containsExactly("OHT-01");
        assertThat(response.blockedEdges()).extracting("edgeId").containsExactly("EDGE-001");
        assertThat(response.recentProblemTransfers()).hasSize(2);
        assertThat(response.recentProblemTransfers().get(0).reason()).isEqualTo("경로 없음");
    }

    @Test
    void 최근_문제_요청은_limit만큼만_반환한다() {
        saveFailedRequest("OHT-01", "첫 번째 실패", now.plusMinutes(1));
        saveFailedRequest("OHT-02", "두 번째 실패", now.plusMinutes(2));
        saveFailedRequest("OHT-03", "세 번째 실패", now.plusMinutes(3));

        var response = operationsService.getOverview(2);

        assertThat(response.recentProblemTransfers()).hasSize(2);
        assertThat(response.recentProblemTransfers().get(0).reason()).isEqualTo("세 번째 실패");
        assertThat(response.recentProblemTransfers().get(1).reason()).isEqualTo("두 번째 실패");
    }

    private void saveWaitingRequest() {
        transferRequestRepository.save(TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL,
                now
        ));
    }

    private void saveFailedRequest(String ohtId, String reason, OffsetDateTime completedAt) {
        TransferRequest request = TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.HIGH,
                completedAt.minusMinutes(1)
        );
        request.assign(ohtId, completedAt.minusSeconds(50));
        request.fail(reason, completedAt);
        transferRequestRepository.save(request);
    }

    private void saveCanceledRequest(String reason, OffsetDateTime completedAt) {
        TransferRequest request = TransferRequest.create(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL,
                completedAt.minusMinutes(1)
        );
        request.cancel(reason, completedAt);
        transferRequestRepository.save(request);
    }
}
