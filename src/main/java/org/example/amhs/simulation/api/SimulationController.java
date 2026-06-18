package org.example.amhs.simulation.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.simulation.application.SimulationService;
import org.example.amhs.simulation.dto.SimulationStartResponse;
import org.example.amhs.simulation.dto.SimulationStatusResponse;
import org.example.amhs.simulation.dto.SimulationStopResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/api/simulation/start")
    ApiResponse<SimulationStartResponse> start() {
        return ApiResponse.ok(simulationService.start());
    }

    @PostMapping("/api/simulation/stop")
    ApiResponse<SimulationStopResponse> stop() {
        return ApiResponse.ok(simulationService.stop());
    }

    @GetMapping("/api/simulation/status")
    ApiResponse<SimulationStatusResponse> getStatus() {
        return ApiResponse.ok(simulationService.getStatus());
    }
}
