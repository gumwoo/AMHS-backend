package org.example.amhs.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class TransferRequestServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

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
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 0, 0, true));
        fabNodeRepository.save(new FabNode("JUNCTION-01", NodeType.JUNCTION, "Junction 01", 1, 0, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 2, 0, true));
        fabNodeRepository.save(new FabNode("CHARGER-01", NodeType.CHARGER, "Charger 01", 3, 0, true));

        fabEdgeRepository.save(new FabEdge("EDGE-001", "STOCKER-A", "JUNCTION-01", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("EDGE-002", "JUNCTION-01", "EQP-01", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("EDGE-003", "CHARGER-01", "STOCKER-A", 5, 5, false));

        ohtRepository.save(Oht.create("OHT-01", "CHARGER-01", now.minusMinutes(10)));
    }

    @Test
    void 반송_요청을_생성한다() {
        TransferRequestResponse response = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.HIGH
        ));

        assertThat(response.requestId()).isNotNull();
        assertThat(response.status()).isEqualTo(TransferRequestStatus.WAITING);
        assertThat(response.priority()).isEqualTo(TransferPriority.HIGH);
    }

    @Test
    void 존재하지_않는_node로_반송_요청을_생성할_수_없다() {
        assertThatThrownBy(() -> transferRequestService.create(new CreateTransferRequestRequest(
                "UNKNOWN",
                "EQP-01",
                TransferPriority.NORMAL
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void 반송_요청_목록과_상세를_조회한다() {
        TransferRequestResponse created = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));

        var page = transferRequestService.search(
                TransferRequestStatus.WAITING,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );
        TransferRequestDetailResponse detail = transferRequestService.get(created.requestId());

        assertThat(page.getContent()).hasSize(1);
        assertThat(detail.requestId()).isEqualTo(created.requestId());
        assertThat(detail.path()).containsExactly("STOCKER-A", "JUNCTION-01", "EQP-01");
    }

    @Test
    void 자동_배정은_요청과_OHT를_같은_트랜잭션에서_변경한다() {
        TransferRequestResponse created = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));

        AssignTransferRequestResponse assigned = transferRequestService.assign(
                created.requestId(),
                new AssignTransferRequestRequest(null)
        );

        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        TransferRequestDetailResponse detail = transferRequestService.get(created.requestId());

        assertThat(assigned.status()).isEqualTo(TransferRequestStatus.ASSIGNED);
        assertThat(assigned.assignedOhtId()).isEqualTo("OHT-01");
        assertThat(assigned.estimatedPath()).containsExactly("CHARGER-01", "STOCKER-A");
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.RESERVED);
        assertThat(oht.getCurrentRequestId()).isEqualTo(created.requestId());
        assertThat(detail.histories()).hasSize(1);
        assertThat(detail.histories().get(0).nextStatus()).isEqualTo(TransferRequestStatus.ASSIGNED);
    }

    @Test
    void 수동_배정할_OHT가_IDLE이_아니면_실패한다() {
        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        oht.markError(now);
        ohtRepository.save(oht);
        TransferRequestResponse created = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));

        assertThatThrownBy(() -> transferRequestService.assign(
                created.requestId(),
                new AssignTransferRequestRequest("OHT-01")
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void ASSIGNED_요청을_취소하면_OHT가_IDLE로_해제된다() {
        TransferRequestResponse created = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));
        transferRequestService.assign(created.requestId(), new AssignTransferRequestRequest("OHT-01"));

        CancelTransferRequestResponse canceled = transferRequestService.cancel(
                created.requestId(),
                new CancelTransferRequestRequest("운영자 취소")
        );

        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        assertThat(canceled.status()).isEqualTo(TransferRequestStatus.CANCELED);
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.IDLE);
        assertThat(oht.getCurrentRequestId()).isNull();
    }
}
