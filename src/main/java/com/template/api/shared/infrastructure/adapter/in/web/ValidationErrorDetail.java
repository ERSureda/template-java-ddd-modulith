package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

/**
 * Detalle inmutable de un error de validación sobre un campo o propiedad específica.
 * <p>
 * Diseñado como {@code record} para máxima eficiencia de memoria y serialización directa.
 *
 * @param field         Nombre del campo o propiedad que originó la violación.
 * @param message       Mensaje descriptivo del motivo del fallo de validación.
 * @param rejectedValue Valor rechazado (opcional; se omite en la serialización si es {@code null}).
 */
public record ValidationErrorDetail(
        String field,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Object rejectedValue
) implements Serializable {

    public ValidationErrorDetail {
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }

    public static ValidationErrorDetail of(String field, String message) {
        return new ValidationErrorDetail(field, message, null);
    }

    public static ValidationErrorDetail of(String field, String message, Object rejectedValue) {
        return new ValidationErrorDetail(field, message, rejectedValue);
    }
}
