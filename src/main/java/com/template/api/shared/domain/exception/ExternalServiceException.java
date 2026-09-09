package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class ExternalServiceException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, ErrorCategory.EXTERNAL_SERVICE, message, cause);
    }

    public ExternalServiceException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.EXTERNAL_SERVICE, message, null);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(CommonError.EXTERNAL_SERVICE_ERROR, ErrorCategory.EXTERNAL_SERVICE, message, cause);
    }
}
