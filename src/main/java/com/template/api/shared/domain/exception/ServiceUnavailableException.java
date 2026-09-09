package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class ServiceUnavailableException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, ErrorCategory.UNAVAILABLE, message, cause);
    }

    public ServiceUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.UNAVAILABLE, message, null);
    }

    public ServiceUnavailableException(String message) {
        super(CommonError.SERVICE_UNAVAILABLE, ErrorCategory.UNAVAILABLE, message, null);
    }
}
