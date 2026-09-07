package com.template.api.shared.domain.exception;

/** A business invariant was violated with otherwise well-formed data. Maps to HTTP 422. */
public final class DomainException extends BaseException {

    public DomainException(String message) {
        this(CommonError.DOMAIN_RULE_VIOLATION, message);
    }

    public DomainException(ErrorCode errorCode, String message, Object... messageArgs) {
        super(errorCode, ErrorCategory.DOMAIN_RULE, message, null, messageArgs);
    }
}
