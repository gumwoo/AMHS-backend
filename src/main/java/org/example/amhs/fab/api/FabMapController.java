package org.example.amhs.fab.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.fab.application.FabMapService;
import org.example.amhs.fab.dto.BlockFabEdgeRequest;
import org.example.amhs.fab.dto.FabEdgeStatusResponse;
import org.example.amhs.fab.dto.FabMapResponse;
import org.example.amhs.operations.application.OperationActionLogService;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FabMapController {

    private final FabMapService fabMapService;
    private final OperationActionLogService operationActionLogService;

    public FabMapController(
            FabMapService fabMapService,
            OperationActionLogService operationActionLogService
    ) {
        this.fabMapService = fabMapService;
        this.operationActionLogService = operationActionLogService;
    }

    @GetMapping("/api/fab-map")
    ApiResponse<FabMapResponse> getFabMap() {
        return ApiResponse.ok(fabMapService.getFabMap());
    }

    @PostMapping("/api/fab-edges/{edgeId}/block")
    ApiResponse<FabEdgeStatusResponse> blockEdge(
            @PathVariable String edgeId,
            @RequestBody(required = false) BlockFabEdgeRequest request
    ) {
        String reason = request == null ? null : request.reason();
        FabEdgeStatusResponse response = fabMapService.blockEdge(edgeId, reason);
        operationActionLogService.record(
                OperationActionType.EDGE_BLOCKED,
                OperationTargetType.EDGE,
                edgeId,
                reason
        );
        return ApiResponse.ok(response);
    }

    @PostMapping("/api/fab-edges/{edgeId}/unblock")
    ApiResponse<FabEdgeStatusResponse> unblockEdge(@PathVariable String edgeId) {
        FabEdgeStatusResponse response = fabMapService.unblockEdge(edgeId);
        operationActionLogService.record(
                OperationActionType.EDGE_UNBLOCKED,
                OperationTargetType.EDGE,
                edgeId,
                "운영자 차단 해제"
        );
        return ApiResponse.ok(response);
    }
}
