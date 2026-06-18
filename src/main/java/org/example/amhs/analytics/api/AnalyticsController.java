package org.example.amhs.analytics.api;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.analytics.application.AnalyticsService;
import org.example.amhs.analytics.dto.AnalyticsSummaryResponse;
import org.example.amhs.analytics.dto.BottleneckResponse;
import org.example.amhs.analytics.dto.OhtThroughputResponse;
import org.example.amhs.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ApiResponse.ok(analyticsService.getSummary(from, to));
    }

    @GetMapping("/oht-throughput")
    public ApiResponse<List<OhtThroughputResponse>> getOhtThroughput(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ApiResponse.ok(analyticsService.getOhtThroughput(from, to));
    }

    @GetMapping("/bottlenecks")
    public ApiResponse<List<BottleneckResponse>> getBottlenecks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(analyticsService.getBottlenecks(from, to, limit));
    }
}
