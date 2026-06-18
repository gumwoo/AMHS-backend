package org.example.amhs.simulation.application;

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
import org.example.amhs.oht.repository.OhtMoveEventRepository;
import org.example.amhs.oht.repository.OhtRepository;
import org.example.amhs.simulation.dto.SimulationStatusResponse;
import org.example.amhs.transfer.application.TransferRequestService;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.dto.AssignTransferRequestRequest;
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
class SimulationServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private TransferRequestService transferRequestService;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Autowired
    private OhtRepository ohtRepository;

    @Autowired
    private OhtMoveEventRepository ohtMoveEventRepository;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        ohtMoveEventRepository.deleteAll();
        transferHistoryRepository.deleteAll();
        transferRequestRepository.deleteAll();
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("CHARGER-01", NodeType.CHARGER, "Charger 01", 0, 0, true));
        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 1, 0, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 2, 0, true));

        fabEdgeRepository.save(new FabEdge("EDGE-001", "CHARGER-01", "STOCKER-A", 5, 5, false));
        fabEdgeRepository.save(new FabEdge("EDGE-002", "STOCKER-A", "EQP-01", 10, 10, false));

        ohtRepository.save(Oht.create("OHT-01", "CHARGER-01", now));
    }

    @Test
    void 시뮬레이션을_시작하고_중지한다() {
        assertThat(simulationService.start().running()).isTrue();
        assertThatThrownBy(() -> simulationService.start()).isInstanceOf(BusinessException.class);

        assertThat(simulationService.stop().running()).isFalse();
        assertThatThrownBy(() -> simulationService.stop()).isInstanceOf(BusinessException.class);
    }

    @Test
    void ASSIGNED_요청을_MOVING으로_시작한다() {
        Long requestId = createAndAssign();

        var response = transferRequestService.start(requestId);

        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        TransferRequest request = transferRequestRepository.findById(requestId).orElseThrow();
        assertThat(response.status()).isEqualTo(TransferRequestStatus.MOVING);
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.MOVING);
        assertThat(request.getStartedAt()).isNotNull();
    }

    @Test
    void tick은_OHT를_출발지와_목적지로_이동시키고_반송을_완료한다() {
        Long requestId = createAndAssign();
        transferRequestService.start(requestId);

        int firstTick = simulationService.tickOnce();
        Oht afterFirstTick = ohtRepository.findById("OHT-01").orElseThrow();
        TransferRequest movingRequest = transferRequestRepository.findById(requestId).orElseThrow();

        assertThat(firstTick).isEqualTo(1);
        assertThat(afterFirstTick.getCurrentNodeId()).isEqualTo("STOCKER-A");
        assertThat(movingRequest.getStatus()).isEqualTo(TransferRequestStatus.MOVING);

        int secondTick = simulationService.tickOnce();
        Oht afterSecondTick = ohtRepository.findById("OHT-01").orElseThrow();
        TransferRequest completedRequest = transferRequestRepository.findById(requestId).orElseThrow();

        assertThat(secondTick).isEqualTo(1);
        assertThat(afterSecondTick.getCurrentNodeId()).isEqualTo("EQP-01");
        assertThat(afterSecondTick.getStatus()).isEqualTo(OhtStatus.IDLE);
        assertThat(afterSecondTick.getCurrentRequestId()).isNull();
        assertThat(completedRequest.getStatus()).isEqualTo(TransferRequestStatus.COMPLETED);
        assertThat(ohtMoveEventRepository.findByRequestIdOrderByOccurredAtAsc(requestId)).hasSize(2);
        assertThat(transferHistoryRepository.findByRequestIdOrderByOccurredAtAsc(requestId)).hasSize(3);
    }

    @Test
    void 이동_중_경로가_막히면_요청을_FAILED로_처리한다() {
        Long requestId = createAndAssign();
        transferRequestService.start(requestId);
        FabEdge edgeToSource = fabEdgeRepository.findById("EDGE-001").orElseThrow();
        edgeToSource.block();
        fabEdgeRepository.save(edgeToSource);

        simulationService.tickOnce();

        TransferRequest failedRequest = transferRequestRepository.findById(requestId).orElseThrow();
        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        assertThat(failedRequest.getStatus()).isEqualTo(TransferRequestStatus.FAILED);
        assertThat(failedRequest.getFailedReason()).isEqualTo("ROUTE_NOT_FOUND");
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.IDLE);
    }

    @Test
    void status는_활성_요청과_OHT_상태를_집계한다() {
        Long requestId = createAndAssign();
        transferRequestService.start(requestId);

        SimulationStatusResponse status = simulationService.getStatus();

        assertThat(status.activeTransferCount()).isEqualTo(1);
        assertThat(status.movingOhtCount()).isEqualTo(1);
        assertThat(status.idleOhtCount()).isZero();
    }

    private Long createAndAssign() {
        var created = transferRequestService.create(new CreateTransferRequestRequest(
                "STOCKER-A",
                "EQP-01",
                TransferPriority.NORMAL
        ));
        transferRequestService.assign(created.requestId(), new AssignTransferRequestRequest("OHT-01"));
        return created.requestId();
    }
}
