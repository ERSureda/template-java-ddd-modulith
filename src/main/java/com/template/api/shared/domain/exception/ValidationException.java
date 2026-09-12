package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;
import com.template.api.shared.domain.error.FieldViolation;

import java.io.Serial;
import java.util.List;

public class ValidationException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<FieldViolation> violations;

    public ValidationException(ErrorCode errorCode, String message, List<FieldViolation> violations) {
        super(errorCode, ErrorCategory.VALIDATION, message);
        this.violations = (violations != null) ? List.copyOf(violations) : List.of();
    }

    public ValidationException(String message, List<FieldViolation> violations) {
        this(CommonError.VALIDATION_ERROR, message, violations);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ValidationException(String message) {
        this(CommonError.VALIDATION_ERROR, message, List.of());
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}