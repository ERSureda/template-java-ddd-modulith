package com.template.api.shared.domain.exception;

/**
 * The domain detected a uniqueness or state-transition conflict. Maps to HTTP 409.
 * Conflicts detected by PostgreSQL are normalized by the web layer, not thrown here.
 */
public final class ConflictException extends BaseException {

    public ConflictException(String message) {
        this(CommonError.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message, Object... messageArgs) {
        super(errorCode, ErrorCategory.CONFLICT, message, null, messageArgs);
    }
}
