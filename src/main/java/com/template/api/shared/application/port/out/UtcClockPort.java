package com.template.api.shared.application.port.out;

import java.time.Instant;

public interface UtcClockPort {
    Instant now();
}
