package com.template.api.shared.domain.error;

import java.util.Objects;

/**
 * Representación inmutable y pura en el dominio de una violación sobre un campo o atributo.
 * <p>
 * Completamente agnóstica a tecnologías, frameworks de transporte o librerías de serialización (DOM-01).
 *
 * @param field   Nombre del campo o atributo que originó la violación.
 * @param message Motivo o regla de negocio infringida.
 */
public record FieldViolation(String field, String message) {

    public FieldViolation {
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }

    public static FieldViolation of(String field, String message) {
        return new FieldViolation(field, message);
    }
}
