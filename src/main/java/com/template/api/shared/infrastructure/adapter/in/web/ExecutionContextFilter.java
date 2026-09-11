package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.infrastructure.context.ExecutionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Filtro HTTP que reconstruye el {@link ExecutionContext} a partir de las cabeceras
 * inyectadas por el API Gateway (Gateway Authentication Offloading) y propaga
 * los identificadores de trazabilidad en MDC y en el portador estático (SHR-03).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ExecutionContextFilter extends OncePerRequestFilter {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        UUID tenantId = parseUuid(request.getHeader(ApiHeaders.TENANT_ID));
        UUID userId = parseUuid(request.getHeader(ApiHeaders.USER_ID));
        Set<String> roles = parseRoles(request);

        ExecutionContext context = new ExecutionContext(tenantId, userId, roles, correlationId);
        ExecutionContextHolder.set(context);

        // Propagar cabecera de correlación en la respuesta HTTP
        response.setHeader(ApiHeaders.CORRELATION_ID, correlationId);

        // Configurar MDC para trazas de logging contextuales
        MDC.put(MDC_CORRELATION_ID, correlationId);
        if (tenantId != null) {
            MDC.put(MDC_TENANT_ID, tenantId.toString());
        }
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId.toString());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ExecutionContextHolder.clear();
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(ApiHeaders.CORRELATION_ID);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    private UUID parseUuid(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(headerValue.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Set<String> parseRoles(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders(ApiHeaders.ROLES);
        if (headers == null || !headers.hasMoreElements()) {
            return Set.of();
        }

        Set<String> roles = new HashSet<>();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (header != null && !header.isBlank()) {
                String[] parts = header.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        roles.add(trimmed);
                    }
                }
            }
        }
        return Set.copyOf(roles);
    }
}