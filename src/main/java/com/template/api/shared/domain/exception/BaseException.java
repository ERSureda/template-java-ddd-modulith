package com.template.api.shared.domain.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Root of the exception hierarchy. The stack trace and suppression policy is derived from the
 * {@link ErrorCategory}, so a business failure cannot pay for diagnostics it does not need and a
 * technical failure cannot lose them.
 *
 * <p><b>Never override {@code fillInStackTrace()}.</b> {@link Throwable} invokes it virtually
 * before subclass fields are assigned, so any override reading own state reads the default and
 * disables the trace exactly where it was meant to be kept.
 */
public abstract sealed class BaseException extends RuntimeException
        permits ConflictException, DomainException, ExternalServiceException, ForbiddenException,
                InfrastructureException, RateLimitExceededException, ResourceNotFoundException,
                ServiceUnavailableException, UnauthorizedException, ValidationException {

    private final ErrorCode errorCode;
    private final ErrorCategory category;
    private final List<Object> messageArgs;

    protected BaseException(
            ErrorCode errorCode,
            ErrorCategory category,
            String message,
            Throwable cause,
            Object... messageArgs) {
        super(message, cause, capturesDiagnostics(category), capturesDiagnostics(category));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.messageArgs = asImmutableList(messageArgs);
    }

    public String getCode() {
        return errorCode.code();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getMessageKey() {
        return errorCode.messageKey();
    }

    /** Positional arguments for {@link #getMessageKey()}. Immutable container, shallow contents. */
    public List<Object> getMessageArgs() {
        return messageArgs;
    }

    private static boolean capturesDiagnostics(ErrorCategory category) {
        return Objects.requireNonNull(category, "category cannot be null").capturesDiagnostics();
    }

    private static List<Object> asImmutableList(Object[] args) {
        return args == null || args.length == 0
                ? List.of()
                : Collections.unmodifiableList(Arrays.asList(args.clone()));
    }
}
