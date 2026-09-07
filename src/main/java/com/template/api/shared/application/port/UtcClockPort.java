package com.template.api.shared.application.port;

import java.time.Instant;

public interface UtcClockPort {
    Instant now();
}
