package com.template.api.shared.domain.exception;

/** The caller is authenticated but lacks the role or tenant membership required. Maps to HTTP 403. */
public final class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        this(CommonError.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message, Object... messageArgs) {
        super(errorCode, ErrorCategory.FORBIDDEN, message, null, messageArgs);
    }
}
