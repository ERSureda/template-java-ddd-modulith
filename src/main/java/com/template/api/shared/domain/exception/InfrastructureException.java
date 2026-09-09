package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class InfrastructureException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InfrastructureException(String message, Throwable cause) {
        super(CommonError.INTERNAL_ERROR, ErrorCategory.INTERNAL, message, cause);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, ErrorCategory.INTERNAL, message, cause);
    }

    public InfrastructureException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.INTERNAL, message, null);
    }
}