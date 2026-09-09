package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class RateLimitExceededException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.RATE_LIMITED, message);
    }

    public RateLimitExceededException(String message) {
        super(CommonError.RATE_LIMIT_EXCEEDED, ErrorCategory.RATE_LIMITED, message);
    }
}
