package com.template.api.shared.infrastructure.web;

import com.taxai.api.shared.application.context.ExecutionContext;
import com.taxai.api.shared.infrastructure.context.ExecutionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Rebuilds the {@link ExecutionContext} from the trusted headers injected by the API Gateway
 * ({@code X-User-Id}, {@code X-Tenant-Id}, {@code X-User-Role}) after Gateway Authentication
 * Offloading (PROJECT_SPEC §4.2). Runs before the {@code DispatcherServlet} and always clears the
 * holder in a {@code finally}, since virtual threads are reused across requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ExecutionContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            ExecutionContextHolder.set(buildContext(request));
            filterChain.doFilter(request, response);
        } finally {
            ExecutionContextHolder.clear();
        }
    }

    private ExecutionContext buildContext(HttpServletRequest request) {
        UUID userId = parseUuid(request.getHeader(ApiHeaders.USER_ID));
        UUID tenantId = parseUuid(request.getHeader(ApiHeaders.TENANT_ID));
        String tenantMembershipRole = request.getHeader(ApiHeaders.USER_ROLE);

        return new ExecutionContext(userId, null, tenantId, tenantMembershipRole);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
