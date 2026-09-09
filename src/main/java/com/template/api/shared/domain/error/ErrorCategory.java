package com.template.api.shared.domain.error;

/**
 * Clasificación semántica de errores a nivel de arquitectura.
 * <p>
 * Determina el tratamiento técnico del error, principalmente si debe
 * capturar o no el stack trace en la JVM. No contiene referencias a protocolos
 * de transporte (HTTP, gRPC, etc.).
 */
public enum ErrorCategory {

    /**
     * Datos de entrada sintáctica o estructuralmente inválidos.
     */
    VALIDATION(false),

    /**
     * El actor no está autenticado o sus credenciales son inválidas.
     */
    UNAUTHENTICATED(false),

    /**
     * El actor está autenticado pero carece de permisos para la operación.
     */
    FORBIDDEN(false),

    /**
     * El recurso o agregado solicitado no existe.
     */
    NOT_FOUND(false),

    /**
     * Estado inconsistente por colisión con el estado actual del sistema (ej. clave única duplicada).
     */
    CONFLICT(false),

    /**
     * Violación de una invariante o regla de negocio del modelo de dominio.
     */
    DOMAIN_RULE(false),

    /**
     * Se ha excedido la cuota de peticiones o límite de tasa permitido.
     */
    RATE_LIMITED(false),

    /**
     * Falla técnica no controlada o error interno de programación.
     */
    INTERNAL(true),

    /**
     * El servicio actual no puede procesar la solicitud temporalmente (ej. sobrecarga, mantenimiento).
     */
    UNAVAILABLE(true),

    /**
     * Falla al comunicarse con un servicio o dependencia externa downstream.
     */
    EXTERNAL_SERVICE(true);

    private final boolean capturesDiagnostics;

    ErrorCategory(boolean capturesDiagnostics) {
        this.capturesDiagnostics = capturesDiagnostics;
    }

    /**
     * Indica si las excepciones asociadas a esta categoría deben recolectar
     * información diagnóstica pesada (stack trace) en la JVM.
     *
     * @return {@code true} para fallas técnicas inesperadas; {@code false} para errores de flujo/negocio.
     */
    public boolean capturesDiagnostics() {
        return capturesDiagnostics;
    }
}