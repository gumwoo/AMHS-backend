package org.example.amhs.common.time;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
