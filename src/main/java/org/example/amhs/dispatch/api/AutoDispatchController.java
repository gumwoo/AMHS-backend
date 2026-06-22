package org.example.amhs.dispatch.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.dispatch.application.AutoDispatchService;
import org.example.amhs.dispatch.dto.AutoDispatchStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutoDispatchController {

    private final AutoDispatchService autoDispatchService;

    public AutoDispatchController(AutoDispatchService autoDispatchService) {
        this.autoDispatchService = autoDispatchService;
    }

    @PostMapping("/api/dispatch/auto/start")
    ApiResponse<AutoDispatchStatusResponse> start() {
        return ApiResponse.ok(autoDispatchService.start());
    }

    @PostMapping("/api/dispatch/auto/stop")
    ApiResponse<AutoDispatchStatusResponse> stop() {
        return ApiResponse.ok(autoDispatchService.stop());
    }

    @PostMapping("/api/dispatch/auto/tick")
    ApiResponse<AutoDispatchStatusResponse> tick() {
        return ApiResponse.ok(autoDispatchService.dispatchOnce());
    }

    @GetMapping("/api/dispatch/auto/status")
    ApiResponse<AutoDispatchStatusResponse> getStatus() {
        return ApiResponse.ok(autoDispatchService.getStatus());
    }
}
