package org.example.amhs.fab.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.fab.application.FabMapService;
import org.example.amhs.fab.dto.BlockFabEdgeRequest;
import org.example.amhs.fab.dto.FabEdgeStatusResponse;
import org.example.amhs.fab.dto.FabMapResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FabMapController {

    private final FabMapService fabMapService;

    public FabMapController(FabMapService fabMapService) {
        this.fabMapService = fabMapService;
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
        return ApiResponse.ok(fabMapService.blockEdge(edgeId, reason));
    }

    @PostMapping("/api/fab-edges/{edgeId}/unblock")
    ApiResponse<FabEdgeStatusResponse> unblockEdge(@PathVariable String edgeId) {
        return ApiResponse.ok(fabMapService.unblockEdge(edgeId));
    }
}
