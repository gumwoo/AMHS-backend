package org.example.amhs.operations.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
import org.example.amhs.operations.repository.OperationActionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OperationActionLogServiceTest {

    @Autowired
    private OperationActionLogService operationActionLogService;

    @Autowired
    private OperationActionLogRepository operationActionLogRepository;

    @BeforeEach
    void setUp() {
        operationActionLogRepository.deleteAll();
    }

    @Test
    void 운영_조치_이력을_저장하고_최근순으로_조회한다() {
        operationActionLogService.record(
                OperationActionType.TRANSFER_CANCELED,
                OperationTargetType.TRANSFER,
                "1001",
                "operator02",
                "운영자 취소"
        );
        operationActionLogService.record(
                OperationActionType.OHT_MARKED_ERROR,
                OperationTargetType.OHT,
                "OHT-01",
                null,
                "오류 처리"
        );

        var logs = operationActionLogService.getRecentLogs();

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).actionType()).isEqualTo(OperationActionType.OHT_MARKED_ERROR);
        assertThat(logs.get(0).operatorId()).isEqualTo("operator01");
        assertThat(logs.get(1).operatorId()).isEqualTo("operator02");
        assertThat(logs.get(1).targetId()).isEqualTo("1001");
    }

    @Test
    void 운영_조치_이력을_조건으로_조회한다() {
        operationActionLogService.record(
                OperationActionType.EDGE_BLOCKED,
                OperationTargetType.EDGE,
                "EDGE-001",
                "operator02",
                "병목 차단"
        );
        operationActionLogService.record(
                OperationActionType.OHT_RECOVERED,
                OperationTargetType.OHT,
                "OHT-01",
                "maintenance01",
                "복구 완료"
        );

        var operatorLogs = operationActionLogService.searchLogs(
                "operator02",
                null,
                null,
                20
        );
        var targetLogs = operationActionLogService.searchLogs(
                null,
                OperationActionType.OHT_RECOVERED,
                "OHT",
                20
        );

        assertThat(operatorLogs).hasSize(1);
        assertThat(operatorLogs.get(0).targetId()).isEqualTo("EDGE-001");
        assertThat(targetLogs).hasSize(1);
        assertThat(targetLogs.get(0).operatorId()).isEqualTo("maintenance01");
    }
}
