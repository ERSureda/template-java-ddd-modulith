package com.template.api.shared.domain.exception;

/**
 * A deterministic technical failure (PostgreSQL, DynamoDB, S3, KMS). Keeps cause and stack trace.
 * Maps to HTTP 500.
 */
public final class InfrastructureException extends BaseException {

    public InfrastructureException(String message) {
        this(CommonError.INFRASTRUCTURE_ERROR, message, null);
    }

    public InfrastructureException(String message, Throwable cause) {
        this(CommonError.INFRASTRUCTURE_ERROR, message, cause);
    }

    public InfrastructureException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public InfrastructureException(
            ErrorCode errorCode, String message, Throwable cause, Object... messageArgs) {
        super(errorCode, ErrorCategory.INTERNAL, message, cause, messageArgs);
    }
}

