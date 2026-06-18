package org.example.amhs.transfer.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.common.response.PageResponse;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.dto.AssignTransferRequestRequest;
import org.example.amhs.transfer.dto.AssignTransferRequestResponse;
import org.example.amhs.transfer.dto.CancelTransferRequestRequest;
import org.example.amhs.transfer.dto.CancelTransferRequestResponse;
import org.example.amhs.transfer.dto.CreateTransferRequestRequest;
import org.example.amhs.transfer.dto.TransferRequestDetailResponse;
import org.example.amhs.transfer.dto.TransferRequestResponse;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

@ActiveProfiles("test")
@SpringBootTest
class TransferRequestControllerTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private TransferRequestController controller;

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
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 0, 0, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 1, 0, true));
        fabNodeRepository.save(new FabNode("CHARGER-01", NodeType.CHARGER, "Charger 01", 2, 0, true));
        fabEdgeRepository.save(new FabEdge("EDGE-001", "STOCKER-A", "EQP-01", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("EDGE-002", "CHARGER-01", "STOCKER-A", 5, 5, false));
        ohtRepository.save(Oht.create("OHT-01", "CHARGER-01", now));
    }

    @Test
    void 반송_요청_API_흐름을_처리한다() {
        ApiResponse<TransferRequestResponse> created = controller.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.HIGH
        ));
        Long requestId = created.data().requestId();

        ApiResponse<PageResponse<TransferRequestResponse>> list = controller.search(
                TransferRequestStatus.WAITING,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );
        ApiResponse<AssignTransferRequestResponse> assigned = controller.assign(
                requestId,
                new AssignTransferRequestRequest(null)
        );
        ApiResponse<TransferRequestDetailResponse> detail = controller.get(requestId);

        assertThat(created.success()).isTrue();
        assertThat(list.data().content()).hasSize(1);
        assertThat(assigned.data().assignedOhtId()).isEqualTo("OHT-01");
        assertThat(detail.data().histories()).hasSize(1);
    }

    @Test
    void 반송_요청을_취소한다() {
        ApiResponse<TransferRequestResponse> created = controller.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));

        ApiResponse<CancelTransferRequestResponse> canceled = controller.cancel(
                created.data().requestId(),
                new CancelTransferRequestRequest("운영자 취소")
        );

        assertThat(canceled.data().status()).isEqualTo(TransferRequestStatus.CANCELED);
        assertThat(canceled.data().reason()).isEqualTo("운영자 취소");
    }
}
