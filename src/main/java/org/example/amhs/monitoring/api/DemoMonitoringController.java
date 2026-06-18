package org.example.amhs.monitoring.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.monitoring.application.DemoMonitoringService;
import org.example.amhs.monitoring.dto.DemoMonitoringActionResponse;
import org.example.amhs.monitoring.dto.DemoMonitoringStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoMonitoringController {

    private final DemoMonitoringService demoMonitoringService;

    public DemoMonitoringController(DemoMonitoringService demoMonitoringService) {
        this.demoMonitoringService = demoMonitoringService;
    }

    @PostMapping("/api/demo-monitoring/start")
    ApiResponse<DemoMonitoringStatusResponse> start() {
        return ApiResponse.ok(demoMonitoringService.start());
    }

    @PostMapping("/api/demo-monitoring/stop")
    ApiResponse<DemoMonitoringStatusResponse> stop() {
        return ApiResponse.ok(demoMonitoringService.stop());
    }

    @PostMapping("/api/demo-monitoring/tick")
    ApiResponse<DemoMonitoringActionResponse> tick() {
        return ApiResponse.ok(demoMonitoringService.tickOnce());
    }

    @GetMapping("/api/demo-monitoring/status")
    ApiResponse<DemoMonitoringStatusResponse> status() {
        return ApiResponse.ok(demoMonitoringService.status());
    }
}
