package org.example.amhs.monitoring.api;

import org.example.amhs.monitoring.application.MonitoringEventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MonitoringController {

    private final MonitoringEventService monitoringEventService;

    public MonitoringController(MonitoringEventService monitoringEventService) {
        this.monitoringEventService = monitoringEventService;
    }

    @GetMapping(value = "/api/monitoring/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() {
        return monitoringEventService.subscribe();
    }
}
