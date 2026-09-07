package com.template.api.shared.domain.exception;

/** The credential is missing, expired or invalid. Maps to HTTP 401 with {@code WWW-Authenticate}. */
public final class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        this(CommonError.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message, Object... messageArgs) {
        super(errorCode, ErrorCategory.UNAUTHENTICATED, message, null, messageArgs);
    }
}
