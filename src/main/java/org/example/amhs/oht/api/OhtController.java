package org.example.amhs.oht.api;

import java.util.List;
import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.oht.application.OhtService;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.dto.OhtDetailResponse;
import org.example.amhs.oht.dto.OhtResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OhtController {

    private final OhtService ohtService;

    public OhtController(OhtService ohtService) {
        this.ohtService = ohtService;
    }

    @GetMapping("/api/ohts")
    ApiResponse<List<OhtResponse>> getOhts(
            @RequestParam(required = false) OhtStatus status,
            @RequestParam(required = false) String currentNodeId
    ) {
        return ApiResponse.ok(ohtService.getOhts(status, currentNodeId));
    }

    @GetMapping("/api/ohts/{ohtId}")
    ApiResponse<OhtDetailResponse> getOht(@PathVariable String ohtId) {
        return ApiResponse.ok(ohtService.getOht(ohtId));
    }

    @PostMapping("/api/ohts/{ohtId}/error")
    ApiResponse<OhtResponse> markError(
            @PathVariable String ohtId,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId
    ) {
        return ApiResponse.ok(ohtService.markError(ohtId, operatorId));
    }

    @PostMapping("/api/ohts/{ohtId}/recover")
    ApiResponse<OhtResponse> recover(
            @PathVariable String ohtId,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId
    ) {
        return ApiResponse.ok(ohtService.recover(ohtId, operatorId));
    }
}
