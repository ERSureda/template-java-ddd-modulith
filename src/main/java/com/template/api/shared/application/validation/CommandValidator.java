package com.template.api.shared.application.validation;

import com.taxai.api.shared.domain.exception.CommonError;
import com.taxai.api.shared.domain.exception.ErrorCode;
import com.taxai.api.shared.domain.exception.ValidationException;

import java.util.*;
import java.util.function.Predicate;

/**
 * Fluent builder for accumulating command validation errors.
 * Designed for use inside compact record constructors or application command handlers.
 *
 * <pre>{@code
 * public CreateVehicleCommand {
 *     CommandValidator.start()
 *         .withCode(FleetError.INVALID_VEHICLE_COMMAND)
 *         .rejectIfBlank(licensePlate, "licensePlate", "is required")
 *         .rejectIfNull(vehicleClass, "vehicleClass", "is required")
 *         .rejectIfInvalidUuid(tenantId, "tenantId", "invalid format")
 *         .validate("CreateVehicleCommand");
 * }
 * }</pre>
 */
public final class CommandValidator {

    private ErrorCode errorCode = CommonError.VALIDATION_ERROR;
    private Map<String, List<String>> errors;

    private CommandValidator() {}

    /** Creates a new validator instance. */
    public static CommandValidator start() {
        return new CommandValidator();
    }

    /** Overrides the default {@link CommonError#VALIDATION_ERROR} with a module-specific code. */
    public CommandValidator withCode(ErrorCode errorCode) {
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        return this;
    }

    /** Adds an error if {@code value} is null or blank. */
    public CommandValidator rejectIfBlank(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            addError(field, message);
        }
        return this;
    }

    /** Adds an error if {@code value} is null. */
    public CommandValidator rejectIfNull(Object value, String field, String message) {
        if (value == null) {
            addError(field, message);
        }
        return this;
    }

    /** Adds an error when {@code condition} is true. */
    public CommandValidator rejectIf(boolean condition, String field, String message) {
        if (condition) {
            addError(field, message);
        }
        return this;
    }

    /** Adds an error if {@code value} is non-null and the predicate evaluates to false. */
    public <T> CommandValidator rejectIfInvalid(T value, Predicate<T> predicate, String field, String message) {
        if (value != null && !predicate.test(value)) {
            addError(field, message);
        }
        return this;
    }

    /** Adds an error if {@code value} is non-null and its stripped length falls outside [min, max]. */
    public CommandValidator rejectIfLengthOutside(String value, int min, int max, String field, String message) {
        if (value != null) {
            int length = value.strip().length();
            if (length < min || length > max) {
                addError(field, message);
            }
        }
        return this;
    }

    /** Adds an error if {@code value} is not blank and is not a canonical UUID. Ignores nulls. */
    public CommandValidator rejectIfInvalidUuid(String value, String field, String message) {
        if (value != null && !value.isBlank()) {
            try {
                UUID.fromString(value.trim());
            } catch (IllegalArgumentException e) {
                addError(field, message);
            }
        }
        return this;
    }

    /**
     * Throws {@link ValidationException} if any errors were accumulated; otherwise returns silently.
     *
     * @param commandName used as the exception message for traceability in logs.
     */
    public void validate(String commandName) {
        if (errors != null && !errors.isEmpty()) {
            throw new ValidationException(errorCode, "Validation failed for " + commandName + ".", errors);
        }
    }

    // ── internals ────────────────────────────────────────────────────────────

    private void addError(String field, String message) {
        if (errors == null) {
            errors = new LinkedHashMap<>();
        }
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }
}

