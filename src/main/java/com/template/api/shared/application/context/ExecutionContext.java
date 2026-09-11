package com.template.api.shared.application.context;

import java.util.Set;
import java.util.UUID;

/**
 * Record inmutable que transporta el contexto de ejecución de la petición (SHR-03).
 *
 * @param tenantId      Identificador del inquilino/organización (opcional).
 * @param userId        Identificador del usuario autenticado (opcional).
 * @param roles         Conjunto inmutable de roles asociados al usuario.
 * @param correlationId Identificador único de correlación para trazabilidad distribuida (obligatorio).
 */
public record ExecutionContext(
        UUID tenantId,
        UUID userId,
        Set<String> roles,
        String correlationId
) {

    public ExecutionContext {
        roles = (roles != null) ? Set.copyOf(roles) : Set.of();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
    }

    /**
     * Factoría para contextos anónimos o no autenticados con un identificador de correlación explícito.
     */
    public static ExecutionContext anonymous(String correlationId) {
        return new ExecutionContext(null, null, Set.of(), correlationId);
    }

    /**
     * Factoría para contextos anónimos con identificador de correlación autogenerado.
     */
    public static ExecutionContext anonymous() {
        return new ExecutionContext(null, null, Set.of(), UUID.randomUUID().toString());
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean hasTenant() {
        return tenantId != null;
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }
}