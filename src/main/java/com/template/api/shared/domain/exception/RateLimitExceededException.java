package com.template.api.shared.domain.exception;

import java.time.Duration;
import java.util.Optional;

/** A throttling quota was exceeded. Maps to HTTP 429 with {@code Retry-After}. */
public final class RateLimitExceededException extends BaseException {

    private final Duration retryAfter;

    public RateLimitExceededException(String message) {
        this(CommonError.RATE_LIMIT_EXCEEDED, message);
    }

    public RateLimitExceededException(ErrorCode errorCode, String message) {
        this(errorCode, message, (Duration) null);
    }

    public RateLimitExceededException(String message, Duration retryAfter) {
        this(CommonError.RATE_LIMIT_EXCEEDED, message, retryAfter);
    }

    public RateLimitExceededException(
            ErrorCode errorCode, String message, Duration retryAfter, Object... messageArgs) {
        super(errorCode, ErrorCategory.RATE_LIMITED, message, null, messageArgs);
        this.retryAfter = RetryAfter.normalize(retryAfter);
    }

    public static RateLimitExceededException retryAfter(String message, Duration retryAfter) {
        return new RateLimitExceededException(CommonError.RATE_LIMIT_EXCEEDED, message, retryAfter);
    }

    public static RateLimitExceededException retryAfter(ErrorCode errorCode, String message, Duration retryAfter) {
        return new RateLimitExceededException(errorCode, message, retryAfter);
    }

    /** Empty when the window is unknown: without it the client retries immediately. */
    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}

