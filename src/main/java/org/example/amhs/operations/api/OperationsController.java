package org.example.amhs.operations.api;

import org.example.amhs.common.response.ApiResponse;
import java.util.List;
import org.example.amhs.operations.application.OperationActionLogService;
import org.example.amhs.operations.application.OperationsService;
import org.example.amhs.operations.dto.OperationActionLogResponse;
import org.example.amhs.operations.dto.OperationsOverviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {

    private final OperationsService operationsService;
    private final OperationActionLogService operationActionLogService;

    public OperationsController(
            OperationsService operationsService,
            OperationActionLogService operationActionLogService
    ) {
        this.operationsService = operationsService;
        this.operationActionLogService = operationActionLogService;
    }

    @GetMapping("/overview")
    public ApiResponse<OperationsOverviewResponse> getOverview(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(operationsService.getOverview(limit));
    }

    @GetMapping("/action-logs")
    public ApiResponse<List<OperationActionLogResponse>> getActionLogs() {
        return ApiResponse.ok(operationActionLogService.getRecentLogs());
    }
}
