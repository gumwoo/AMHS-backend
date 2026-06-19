package org.example.amhs.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.example.amhs.transfer.dto.CreateTransferRequestRequest;
import org.example.amhs.transfer.dto.TransferRequestResponse;
import org.example.amhs.transfer.repository.TransferHistoryRepository;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class TransferRequestConcurrencyTest {

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
    void concurrent_assign_requests_reserve_only_one_oht() throws Exception {
        int requestCount = 100;
        List<Long> requestIds = createWaitingRequests(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(requestCount);

        try {
            for (Long requestId : requestIds) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        transferRequestService.assign(requestId, new AssignTransferRequestRequest("OHT-01"));
                        successCount.incrementAndGet();
                    } catch (BusinessException exception) {
                        failedCount.incrementAndGet();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        failedCount.incrementAndGet();
                    }
                });
            }

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        Oht oht = ohtRepository.findById("OHT-01").orElseThrow();
        long assignedRequests = transferRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() == TransferRequestStatus.ASSIGNED)
                .count();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failedCount.get()).isEqualTo(requestCount - 1);
        assertThat(assignedRequests).isEqualTo(1);
        assertThat(oht.getStatus()).isEqualTo(OhtStatus.RESERVED);
        assertThat(oht.getCurrentRequestId()).isNotNull();
    }

    private List<Long> createWaitingRequests(int requestCount) {
        List<Long> requestIds = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            TransferRequestResponse created = transferRequestService.create(new CreateTransferRequestRequest(
                    "STOCKER-A",
                    "EQP-01",
                    TransferPriority.NORMAL
            ));
            requestIds.add(created.requestId());
        }
        return requestIds;
    }
}
