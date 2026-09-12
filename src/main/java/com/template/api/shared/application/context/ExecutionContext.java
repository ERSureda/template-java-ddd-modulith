package com.template.api.shared.application.context;

import java.util.Set;
import java.util.UUID;

/**
 * Record inmutable que transporta el contexto de ejecución de la petición (SHR-03).
 *
 * @param tenantId Identificador del inquilino/organización (opcional).
 * @param userId   Identificador del usuario autenticado (opcional).
 * @param roles    Conjunto inmutable de roles asociados al usuario.
 */
public record ExecutionContext(
        UUID tenantId,
        UUID userId,
        Set<String> roles
) {

    public ExecutionContext {
        roles = (roles != null) ? Set.copyOf(roles) : Set.of();
    }

    public static ExecutionContext anonymous() {
        return new ExecutionContext(null, null, Set.of());
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