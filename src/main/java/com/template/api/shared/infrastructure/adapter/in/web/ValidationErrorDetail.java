package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;
import java.util.Set;

/**
 * Detalle inmutable de un error de validación sobre un campo o propiedad específica.
 * <p>
 * Incluye sanitización preventiva conforme a directivas de seguridad OWASP para evitar
 * fugas de información sensible (contraseñas, tokens) y ataques DoS por payloads excesivos.
 */
public record ValidationErrorDetail(
        String field,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Object rejectedValue
) {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "secret", "token", "pin", "cvv", "credential", "apikey", "authorization"
    );

    private static final int MAX_VALUE_LENGTH = 100;
    private static final String MASKED_VALUE = "[PROTECTED]";

    public ValidationErrorDetail {
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }

    public static ValidationErrorDetail of(String field, String message) {
        return new ValidationErrorDetail(field, message, null);
    }

    public static ValidationErrorDetail of(String field, String message, Object rejectedValue) {
        return new ValidationErrorDetail(field, message, sanitize(field, rejectedValue));
    }

    private static Object sanitize(String field, Object value) {
        if (value == null) {
            return null;
        }

        // 1. Protección OWASP: No reflejar contraseñas o credenciales en el JSON de error
        String lowerField = field.toLowerCase();
        for (String sensitive : SENSITIVE_FIELDS) {
            if (lowerField.contains(sensitive)) {
                return MASKED_VALUE;
            }
        }

        // 2. Protección DoS: Evitar serializar cadenas gigantescas enviadas por atacantes
        if (value instanceof String str && str.length() > MAX_VALUE_LENGTH) {
            return str.substring(0, MAX_VALUE_LENGTH) + "... [truncated]";
        }

        return value;
    }
}
