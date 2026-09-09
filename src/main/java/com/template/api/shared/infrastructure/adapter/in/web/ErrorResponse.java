package com.template.api.shared.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Objects;

/**
 * Contrato canónico ultraligero para todas las respuestas de error de la API REST.
 * <p>
 * Diseñado como {@code record} inmutable de coste mínimo de memoria y serialización directa.
 */
public record ErrorResponse(
        int status,
        String code,
        String detail,
        List<ValidationErrorDetail> errors
) {

    public static final List<ValidationErrorDetail> NO_ERRORS = List.of();

    public ErrorResponse {
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(detail, "detail cannot be null");
        errors = (errors == null || errors.isEmpty()) ? NO_ERRORS : List.copyOf(errors);
    }

    public ErrorResponse(int status, String code, String detail) {
        this(status, code, detail, NO_ERRORS);
    }

    public static ErrorResponse of(int status, String code, String detail) {
        return new ErrorResponse(status, code, detail, NO_ERRORS);
    }

    public static ErrorResponse of(int status, String code, String detail, List<ValidationErrorDetail> errors) {
        return new ErrorResponse(status, code, detail, errors);
    }
}
