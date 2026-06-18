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
                "운영자 취소"
        );
        operationActionLogService.record(
                OperationActionType.OHT_MARKED_ERROR,
                OperationTargetType.OHT,
                "OHT-01",
                "오류 처리"
        );

        var logs = operationActionLogService.getRecentLogs();

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).actionType()).isEqualTo(OperationActionType.OHT_MARKED_ERROR);
        assertThat(logs.get(0).operatorId()).isEqualTo("operator01");
        assertThat(logs.get(1).targetId()).isEqualTo("1001");
    }
}
