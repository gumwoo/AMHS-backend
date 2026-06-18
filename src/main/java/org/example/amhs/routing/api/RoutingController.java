package org.example.amhs.routing.api;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.routing.application.RoutingService;
import org.example.amhs.routing.dto.RouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/api/routes/shortest")
    ApiResponse<RouteResponse> findShortestRoute(
            @RequestParam String sourceNodeId,
            @RequestParam String destinationNodeId
    ) {
        return ApiResponse.ok(routingService.findShortestPath(sourceNodeId, destinationNodeId).toResponse());
    }
}
