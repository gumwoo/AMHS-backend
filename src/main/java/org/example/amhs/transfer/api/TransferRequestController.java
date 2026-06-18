package org.example.amhs.transfer.api;

import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.common.response.PageResponse;
import org.example.amhs.transfer.application.TransferRequestService;
import org.example.amhs.transfer.domain.TransferPriority;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.example.amhs.transfer.dto.AssignTransferRequestRequest;
import org.example.amhs.transfer.dto.AssignTransferRequestResponse;
import org.example.amhs.transfer.dto.CancelTransferRequestRequest;
import org.example.amhs.transfer.dto.CancelTransferRequestResponse;
import org.example.amhs.transfer.dto.CreateTransferRequestRequest;
import org.example.amhs.transfer.dto.StartTransferRequestResponse;
import org.example.amhs.transfer.dto.TransferRequestDetailResponse;
import org.example.amhs.transfer.dto.TransferRequestResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    public TransferRequestController(TransferRequestService transferRequestService) {
        this.transferRequestService = transferRequestService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/transfer-requests")
    ApiResponse<TransferRequestResponse> create(@Valid @RequestBody CreateTransferRequestRequest request) {
        return ApiResponse.ok(transferRequestService.create(request));
    }

    @GetMapping("/api/transfer-requests")
    ApiResponse<PageResponse<TransferRequestResponse>> search(
            @RequestParam(required = false) TransferRequestStatus status,
            @RequestParam(required = false) TransferPriority priority,
            @RequestParam(required = false) String assignedOhtId,
            @RequestParam(required = false) String sourceNodeId,
            @RequestParam(required = false) String destinationNodeId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(PageResponse.from(transferRequestService.search(
                status,
                priority,
                assignedOhtId,
                sourceNodeId,
                destinationNodeId,
                from,
                to,
                pageable
        )));
    }

    @GetMapping("/api/transfer-requests/{requestId}")
    ApiResponse<TransferRequestDetailResponse> get(@PathVariable Long requestId) {
        return ApiResponse.ok(transferRequestService.get(requestId));
    }

    @PostMapping("/api/transfer-requests/{requestId}/assign")
    ApiResponse<AssignTransferRequestResponse> assign(
            @PathVariable Long requestId,
            @RequestBody(required = false) AssignTransferRequestRequest request
    ) {
        return ApiResponse.ok(transferRequestService.assign(requestId, request));
    }

    @PostMapping("/api/transfer-requests/{requestId}/start")
    ApiResponse<StartTransferRequestResponse> start(@PathVariable Long requestId) {
        return ApiResponse.ok(transferRequestService.start(requestId));
    }

    @PostMapping("/api/transfer-requests/{requestId}/cancel")
    ApiResponse<CancelTransferRequestResponse> cancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) CancelTransferRequestRequest request
    ) {
        return ApiResponse.ok(transferRequestService.cancel(requestId, request));
    }
}
