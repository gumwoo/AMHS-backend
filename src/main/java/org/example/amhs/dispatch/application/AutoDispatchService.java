package org.example.amhs.dispatch.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.amhs.common.time.TimeProvider;
import org.example.amhs.dispatch.dto.AutoDispatchStatusResponse;
import org.example.amhs.transfer.application.TransferRequestService;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.dto.AssignTransferRequestRequest;
import org.example.amhs.transfer.repository.TransferRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoDispatchService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final TransferRequestRepository transferRequestRepository;
    private final TransferRequestService transferRequestService;
    private final TimeProvider timeProvider;
    private final int scanLimit;

    private OffsetDateTime lastTickAt;
    private int lastScannedCount;
    private int lastAssignedCount;
    private int lastSkippedCount;

    public AutoDispatchService(
            TransferRequestRepository transferRequestRepository,
            TransferRequestService transferRequestService,
            TimeProvider timeProvider,
            @Value("${amhs.dispatch.scan-limit:20}") int scanLimit
    ) {
        this.transferRequestRepository = transferRequestRepository;
        this.transferRequestService = transferRequestService;
        this.timeProvider = timeProvider;
        this.scanLimit = Math.max(1, scanLimit);
    }

    public AutoDispatchStatusResponse start() {
        running.set(true);
        return getStatus();
    }

    public AutoDispatchStatusResponse stop() {
        running.set(false);
        return getStatus();
    }

    public AutoDispatchStatusResponse getStatus() {
        return new AutoDispatchStatusResponse(
                running.get(),
                scanLimit,
                lastTickAt,
                lastScannedCount,
                lastAssignedCount,
                lastSkippedCount
        );
    }

    @Scheduled(fixedDelayString = "${amhs.dispatch.tick-interval-ms:1000}")
    public void scheduledTick() {
        if (running.get()) {
            dispatchOnce();
        }
    }

    @Transactional(readOnly = true)
    public List<TransferRequest> findSchedulableRequests() {
        return transferRequestRepository.findSchedulableRequests(PageRequest.of(0, scanLimit));
    }

    public AutoDispatchStatusResponse dispatchOnce() {
        lastTickAt = timeProvider.now();
        List<TransferRequest> waitingRequests = findSchedulableRequests();
        int assignedCount = 0;
        int skippedCount = 0;

        for (TransferRequest request : waitingRequests) {
            try {
                transferRequestService.assign(request.getRequestId(), new AssignTransferRequestRequest(null));
                assignedCount++;
            } catch (RuntimeException exception) {
                skippedCount++;
            }
        }

        lastScannedCount = waitingRequests.size();
        lastAssignedCount = assignedCount;
        lastSkippedCount = skippedCount;
        return getStatus();
    }
}
