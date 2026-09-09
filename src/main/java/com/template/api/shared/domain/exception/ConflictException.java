package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class ConflictException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.CONFLICT, message);
    }

    public ConflictException(String message) {
        super(CommonError.RESOURCE_ALREADY_EXISTS, ErrorCategory.CONFLICT, message);
    }
}
