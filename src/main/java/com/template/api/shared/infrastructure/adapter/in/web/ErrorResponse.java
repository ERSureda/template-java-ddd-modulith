package com.template.api.shared.infrastructure.adapter.in.web;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Contrato canónico ultraligero para todas las respuestas de error de la API REST.
 * <p>
 * Implementa la directiva normativa {@code INP-03} y la decisión de arquitectura {@code ADR-03}
 * sustituyendo esquemas verbosos (RFC 7807) por un formato lean de alta eficiencia:
 * <pre>{@code
 * {
 *   "status": 400,
 *   "code": "VALIDATION_FAILED",
 *   "detail": "Descripción del fallo funcional o de contrato",
 *   "errors": []
 * }
 * }</pre>
 *
 * @param status Código de estado HTTP numérico (e.g. 400, 404, 409, 500).
 * @param code   Código alfanumérico estable de error (convención: SCREAMING_SNAKE_CASE).
 * @param detail Mensaje descriptivo comprensible para el cliente o descripción funcional.
 * @param errors Colección de fallos específicos por campo (vacía por defecto, nunca {@code null}).
 */
public record ErrorResponse(
        int status,
        String code,
        String detail,
        List<ValidationErrorDetail> errors
) implements Serializable {

    public ErrorResponse {
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(detail, "detail cannot be null");
        errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public ErrorResponse(int status, String code, String detail) {
        this(status, code, detail, List.of());
    }

    public static ErrorResponse of(int status, String code, String detail) {
        return new ErrorResponse(status, code, detail, List.of());
    }

    public static ErrorResponse of(int status, String code, String detail, List<ValidationErrorDetail> errors) {
        return new ErrorResponse(status, code, detail, errors);
    }
}
