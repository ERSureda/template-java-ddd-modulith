package com.template.api.shared.domain.exception;

import java.time.Duration;

/**
 * Normalizes retry windows for {@link RateLimitExceededException} and
 * {@link ServiceUnavailableException}.
 *
 * <p>HTTP {@code Retry-After} requires a non-negative integer or HTTP date. A non-positive or zero
 * duration would generate an invalid header or trigger immediate tight loops, so it is clamped to 1 second.
 */
final class RetryAfter {

    private static final Duration MINIMUM = Duration.ofSeconds(1);

    private RetryAfter() {
        // Utility class
    }

    static Duration normalize(Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.isNegative() || duration.isZero() ? MINIMUM : duration;
    }
}
