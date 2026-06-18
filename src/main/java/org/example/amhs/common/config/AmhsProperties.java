package org.example.amhs.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amhs")
public record AmhsProperties(
        Monitoring monitoring,
        Simulation simulation,
        Analytics analytics
) {

    public record Monitoring(
            long sseTimeoutMs,
            long heartbeatIntervalMs,
            int outboundQueueSize
    ) {
    }

    public record Simulation(
            long tickIntervalMs,
            int maxActiveTransfers,
            int moveEventBufferSize
    ) {
    }

    public record Analytics(
            long delayThresholdSeconds
    ) {
    }
}
