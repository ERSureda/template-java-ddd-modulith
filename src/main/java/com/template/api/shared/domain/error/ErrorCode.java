package com.template.api.shared.domain.error;

/**
 * Contrato minimalista para identificadores únicos de error.
 * <p>
 * Diseñado como interfaz funcional para que cualquier enum en los módulos de negocio
 * pueda implementarla de forma natural retornando {@code name()}.
 */
public interface ErrorCode {

    /**
     * Identificador alfanumérico único del error (convención: SCREAMING_SNAKE_CASE).
     */
    String code();
}
