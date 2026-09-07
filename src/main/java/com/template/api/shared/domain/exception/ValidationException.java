package com.template.api.shared.domain.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Input is malformed or incomplete. Maps to HTTP 400, carrying the failures per field. */
public final class ValidationException extends BaseException {

    private final Map<String, List<String>> validationErrors;

    public ValidationException(String message) {
        this(CommonError.VALIDATION_ERROR, message);
    }

    public ValidationException(ErrorCode errorCode, String message, Object... messageArgs) {
        this(errorCode, message, Map.of(), messageArgs);
    }

    public ValidationException(String message, Map<String, List<String>> validationErrors) {
        this(CommonError.VALIDATION_ERROR, message, validationErrors);
    }

    public ValidationException(
            ErrorCode errorCode,
            String message,
            Map<String, List<String>> validationErrors,
            Object... messageArgs) {
        super(errorCode, ErrorCategory.VALIDATION, message, null, messageArgs);
        this.validationErrors = asImmutableMap(validationErrors);
    }

    public static ValidationException forField(String field, String error) {
        return forField(CommonError.VALIDATION_ERROR, field, error);
    }

    public static ValidationException forField(ErrorCode errorCode, String field, String error) {
        return new ValidationException(
                errorCode, "Validation failed for field: " + field, Map.of(field, List.of(error)));
    }

    public static ValidationException fromFieldErrors(Map<String, String> fieldErrors) {
        return fromFieldErrors(CommonError.VALIDATION_ERROR, fieldErrors);
    }

    public static ValidationException fromFieldErrors(ErrorCode errorCode, Map<String, String> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return new ValidationException(errorCode, "Validation failed", Map.of());
        }
        Map<String, List<String>> multiMap = new LinkedHashMap<>(fieldErrors.size());
        fieldErrors.forEach((field, msg) -> multiMap.put(field, msg == null ? List.of() : List.of(msg)));
        return new ValidationException(errorCode, "Validation failed", multiMap);
    }

    /** Field name to its failures. Never null, deeply immutable. */
    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    private static Map<String, List<String>> asImmutableMap(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>(source.size());
        source.forEach((field, errors) -> copy.put(field, errors == null ? List.of() : List.copyOf(errors)));
        return Map.copyOf(copy);
    }
}
