package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class UnauthenticatedException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnauthenticatedException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.UNAUTHENTICATED, message);
    }

    public UnauthenticatedException(String message) {
        super(CommonError.UNAUTHENTICATED_ACCESS, ErrorCategory.UNAUTHENTICATED, message);
    }
}
