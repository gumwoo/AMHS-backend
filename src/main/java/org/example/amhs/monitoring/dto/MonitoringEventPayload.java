package org.example.amhs.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.time.OffsetDateTime;
import java.util.Map;
import org.example.amhs.monitoring.event.DomainEventType;

public class MonitoringEventPayload {

    private final String eventId;
    private final DomainEventType eventType;
    private final OffsetDateTime occurredAt;
    private final Map<String, Object> data;

    public MonitoringEventPayload(
            String eventId,
            DomainEventType eventType,
            OffsetDateTime occurredAt,
            Map<String, Object> data
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.data = data;
    }

    public String eventId() {
        return eventId;
    }

    public DomainEventType eventType() {
        return eventType;
    }

    public OffsetDateTime occurredAt() {
        return occurredAt;
    }

    @JsonAnyGetter
    public Map<String, Object> data() {
        return data;
    }
}
