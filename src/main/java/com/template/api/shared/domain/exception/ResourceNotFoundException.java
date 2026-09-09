package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;

public class ResourceNotFoundException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public <T> ResourceNotFoundException(Class<?> entityClass, T id) {
        super(
                CommonError.RESOURCE_NOT_FOUND,
                ErrorCategory.NOT_FOUND,
                String.format("The entity '%s' with ID '%s' does not exist.", entityClass.getSimpleName(), id.toString())
        );
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String message) {
        super(CommonError.RESOURCE_NOT_FOUND, ErrorCategory.NOT_FOUND, message);
    }
}
