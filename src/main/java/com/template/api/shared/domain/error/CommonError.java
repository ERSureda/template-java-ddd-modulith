package com.template.api.shared.domain.error;

public enum CommonError implements ErrorCode {

    // =========================================================================
    // VALIDATION (ErrorCategory.VALIDATION)
    // =========================================================================
    /** Error genérico de validación de formato, estructura o contrato sintáctico. */
    VALIDATION_ERROR,
    /** Se omitió un campo o parámetro obligatorio en la solicitud. */
    MISSING_REQUIRED_FIELD,

    // =========================================================================
    // AUTHENTICATION & AUTHORIZATION (UNAUTHENTICATED / FORBIDDEN)
    // =========================================================================
    /** El solicitante no está autenticado o sus credenciales expiraron/son inválidas. */
    UNAUTHENTICATED_ACCESS,
    /** El solicitante está autenticado pero no tiene privilegios para esta acción. */
    ACCESS_DENIED,

    // =========================================================================
    // RESOURCE EXISTENCE (ErrorCategory.NOT_FOUND)
    // =========================================================================
    /** La entidad, recurso o agregado solicitado no existe. */
    RESOURCE_NOT_FOUND,

    // =========================================================================
    // CONFLICTS & CONCURRENCY (ErrorCategory.CONFLICT)
    // =========================================================================
    /** Intento de crear un recurso que colisiona con uno existente (clave única, email, etc.). */
    RESOURCE_ALREADY_EXISTS,
    /** Conflicto de bloqueo optimista o modificación concurrente de estado. */
    CONCURRENT_MODIFICATION,

    // =========================================================================
    // DOMAIN INVARIANTS (ErrorCategory.DOMAIN_RULE)
    // =========================================================================
    /** Violación de una invariante o regla de negocio del modelo de dominio. */
    DOMAIN_RULE_VIOLATION,

    // =========================================================================
    // TRAFFIC & RATE LIMITING (ErrorCategory.RATE_LIMITED)
    // =========================================================================
    /** Se ha superado el límite de solicitudes o cuota permitida en la ventana de tiempo. */
    RATE_LIMIT_EXCEEDED,

    // =========================================================================
    // TECHNICAL & INFRASTRUCTURE (INTERNAL / UNAVAILABLE / EXTERNAL_SERVICE)
    // =========================================================================
    /** Falla técnica no controlada, bug en tiempo de ejecución o estado inconsistente del sistema. */
    INTERNAL_ERROR,
    /** El servicio no puede atender la petición en este momento (mantenimiento, saturación). */
    SERVICE_UNAVAILABLE,
    /** Fallo o timeout en la integración con un servicio downstream o API de terceros. */
    EXTERNAL_SERVICE_ERROR;

    @Override
    public String code() {
        return name();
    }
}
