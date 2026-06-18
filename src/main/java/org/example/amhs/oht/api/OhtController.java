package org.example.amhs.oht.api;

import java.util.List;
import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.operations.application.OperationActionLogService;
import org.example.amhs.operations.domain.OperationActionType;
import org.example.amhs.operations.domain.OperationTargetType;
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
    private final OperationActionLogService operationActionLogService;

    public OhtController(
            OhtService ohtService,
            OperationActionLogService operationActionLogService
    ) {
        this.ohtService = ohtService;
        this.operationActionLogService = operationActionLogService;
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
        OhtResponse response = ohtService.markError(ohtId);
        operationActionLogService.record(
                OperationActionType.OHT_MARKED_ERROR,
                OperationTargetType.OHT,
                ohtId,
                operatorId,
                "운영자 오류 처리"
        );
        return ApiResponse.ok(response);
    }

    @PostMapping("/api/ohts/{ohtId}/recover")
    ApiResponse<OhtResponse> recover(
            @PathVariable String ohtId,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId
    ) {
        OhtResponse response = ohtService.recover(ohtId);
        operationActionLogService.record(
                OperationActionType.OHT_RECOVERED,
                OperationTargetType.OHT,
                ohtId,
                operatorId,
                "운영자 복구 처리"
        );
        return ApiResponse.ok(response);
    }
}
