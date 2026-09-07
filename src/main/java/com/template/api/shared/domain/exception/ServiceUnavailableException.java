package com.template.api.shared.domain.exception;

import java.time.Duration;
import java.util.Optional;

/**
 * A transient, retryable outage (Valkey down, pool exhausted, circuit open).
 * Maps to HTTP 503 with {@code Retry-After}.
 */
public final class ServiceUnavailableException extends BaseException {

    private final Duration retryAfter;

    public ServiceUnavailableException(String message) {
        this(CommonError.SERVICE_UNAVAILABLE, message, null);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        this(CommonError.SERVICE_UNAVAILABLE, message, cause);
    }

    public ServiceUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    public ServiceUnavailableException(String message, Throwable cause, Duration retryAfter) {
        this(CommonError.SERVICE_UNAVAILABLE, message, cause, retryAfter);
    }

    public ServiceUnavailableException(
            ErrorCode errorCode,
            String message,
            Throwable cause,
            Duration retryAfter,
            Object... messageArgs) {
        super(errorCode, ErrorCategory.UNAVAILABLE, message, cause, messageArgs);
        this.retryAfter = RetryAfter.normalize(retryAfter);
    }

    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}

