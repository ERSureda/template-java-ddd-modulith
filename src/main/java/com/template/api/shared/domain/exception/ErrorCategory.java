package com.template.api.shared.domain.exception;

/**
 * Nature of a failure, decoupled from HTTP. Owns the diagnostic and retry policy so it cannot
 * drift between the exception hierarchy, the log level and the outbox relay.
 */
public enum ErrorCategory {

    VALIDATION(false, false),
    UNAUTHENTICATED(false, false),
    FORBIDDEN(false, false),
    NOT_FOUND(false, false),
    CONFLICT(false, false),
    DOMAIN_RULE(false, false),
    RATE_LIMITED(false, true),
    EXTERNAL_SERVICE(true, true),
    UNAVAILABLE(true, true),
    INTERNAL(true, false);

    private final boolean capturesDiagnostics;
    private final boolean retryable;

    ErrorCategory(boolean capturesDiagnostics, boolean retryable) {
        this.capturesDiagnostics = capturesDiagnostics;
        this.retryable = retryable;
    }

    /**
     * Whether a failure of this nature must carry a stack trace and suppressed exceptions.
     * Expected business flows do not: {@code fillInStackTrace()} walks the whole call stack and
     * contributes nothing to a 404 or a validation error.
     */
    public boolean capturesDiagnostics() {
        return capturesDiagnostics;
    }

    /** Whether retrying the same operation can plausibly succeed. */
    public boolean isRetryable() {
        return retryable;
    }
}
