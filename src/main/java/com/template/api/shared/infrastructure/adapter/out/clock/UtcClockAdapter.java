package com.template.api.shared.infrastructure.adapter.out.clock;

import com.template.api.shared.application.port.out.UtcClockPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Adaptador de salida para la provisión centralizada de tiempo en formato UTC.
 */
@Component
public final class UtcClockAdapter implements UtcClockPort {

    private final Clock clock;

    public UtcClockAdapter() {
        this(Clock.systemUTC());
    }

    public UtcClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
