package org.example.amhs.dispatch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.transfer.application.TransferRequestService;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequestStatus;
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
class AutoDispatchServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private AutoDispatchService autoDispatchService;

    @Autowired
    private TransferRequestService transferRequestService;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Autowired
    private OhtRepository ohtRepository;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        autoDispatchService.stop();
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 0, 0, true));
        fabNodeRepository.save(new FabNode("JUNCTION-01", NodeType.JUNCTION, "Junction 01", 1, 0, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 2, 0, true));
        fabNodeRepository.save(new FabNode("CHARGER-01", NodeType.CHARGER, "Charger 01", 3, 0, true));

        fabEdgeRepository.save(new FabEdge("EDGE-001", "CHARGER-01", "STOCKER-A", 5, 5, false));
        fabEdgeRepository.save(new FabEdge("EDGE-002", "STOCKER-A", "JUNCTION-01", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("EDGE-003", "JUNCTION-01", "EQP-01", 10, 10, false));

        ohtRepository.save(Oht.create("OHT-01", "CHARGER-01", now.minusMinutes(10)));
    }

    @Test
    void WAITING_작업을_우선순위_순서로_자동_배정한다() {
        var normal = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));
        var urgent = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.URGENT
        ));

        var status = autoDispatchService.dispatchOnce();

        var urgentRequest = transferRequestRepository.findById(urgent.requestId()).orElseThrow();
        var normalRequest = transferRequestRepository.findById(normal.requestId()).orElseThrow();
        var oht = ohtRepository.findById("OHT-01").orElseThrow();

        assertThat(status.lastScannedCount()).isEqualTo(2);
        assertThat(status.lastAssignedCount()).isEqualTo(1);
        assertThat(status.lastSkippedCount()).isEqualTo(1);
        assertThat(urgentRequest.getStatus()).isEqualTo(TransferRequestStatus.ASSIGNED);
        assertThat(urgentRequest.getAssignedOhtId()).isEqualTo("OHT-01");
        assertThat(normalRequest.getStatus()).isEqualTo(TransferRequestStatus.WAITING);
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.RESERVED);
        assertThat(oht.getCurrentRequestId()).isEqualTo(urgent.requestId());
    }
}
