package org.example.amhs.operations.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.operations.application.OperationsService;
import org.example.amhs.operations.dto.OperationsOverviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {

    private final OperationsService operationsService;

    public OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/overview")
    public ApiResponse<OperationsOverviewResponse> getOverview(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(operationsService.getOverview(limit));
    }
}
