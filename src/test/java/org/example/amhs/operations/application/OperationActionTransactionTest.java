package org.example.amhs.operations.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.example.amhs.fab.application.FabMapService;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.application.OhtService;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.repository.OperationActionLogRepository;
import org.example.amhs.transfer.application.TransferRequestService;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.dto.CancelTransferRequestRequest;
import org.example.amhs.transfer.dto.CreateTransferRequestRequest;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OperationActionTransactionTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private FabMapService fabMapService;

    @Autowired
    private OhtService ohtService;

    @Autowired
    private TransferRequestService transferRequestService;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @Autowired
    private OhtRepository ohtRepository;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Autowired
    private OperationActionLogRepository operationActionLogRepository;

    @BeforeEach
    void setUp() {
        operationActionLogRepository.deleteAll();
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 0, 0, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 1, 0, true));
        fabEdgeRepository.save(new FabEdge("EDGE-001", "STOCKER-A", "EQP-01", 10, 10, false));
        ohtRepository.save(Oht.create("OHT-01", "STOCKER-A", BASE_TIME));
    }

    @Test
    void edgeBlockAndAuditLogAreSavedInOneOperationFlow() {
        fabMapService.blockEdge("EDGE-001", "maintenance", "operator02");

        assertThat(fabEdgeRepository.findById("EDGE-001")).hasValueSatisfying(edge ->
                assertThat(edge.isBlocked()).isTrue()
        );
        assertThat(operationActionLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getActionType()).isEqualTo(OperationActionType.EDGE_BLOCKED);
                    assertThat(log.getTargetId()).isEqualTo("EDGE-001");
                    assertThat(log.getOperatorId()).isEqualTo("operator02");
                    assertThat(log.getReason()).isEqualTo("maintenance");
                });
    }

    @Test
    void ohtErrorAndAuditLogAreSavedInOneOperationFlow() {
        ohtService.markError("OHT-01", "operator03");

        assertThat(ohtRepository.findById("OHT-01")).hasValueSatisfying(oht ->
                assertThat(oht.getStatus()).isEqualTo(OhtStatus.ERROR)
        );
        assertThat(operationActionLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getActionType()).isEqualTo(OperationActionType.OHT_MARKED_ERROR);
                    assertThat(log.getTargetId()).isEqualTo("OHT-01");
                    assertThat(log.getOperatorId()).isEqualTo("operator03");
                });
    }

    @Test
    void transferCancelAndAuditLogAreSavedInOneOperationFlow() {
        Long requestId = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.HIGH
        )).requestId();
        operationActionLogRepository.deleteAll();

        transferRequestService.cancel(requestId, new CancelTransferRequestRequest("operator cancel"), "operator04");

        assertThat(transferRequestRepository.findById(requestId)).hasValueSatisfying(request ->
                assertThat(request.getStatus()).isEqualTo(TransferRequestStatus.CANCELED)
        );
        assertThat(operationActionLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getActionType()).isEqualTo(OperationActionType.TRANSFER_CANCELED);
                    assertThat(log.getTargetId()).isEqualTo(String.valueOf(requestId));
                    assertThat(log.getOperatorId()).isEqualTo("operator04");
                    assertThat(log.getReason()).isEqualTo("operator cancel");
                });
    }
}
