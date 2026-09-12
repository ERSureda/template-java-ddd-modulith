package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.infrastructure.context.ExecutionContextHolder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionContextFilter Unit Tests")
class ExecutionContextFilterTest {

    private ExecutionContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ExecutionContextFilter();
        ExecutionContextHolder.clear();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        ExecutionContextHolder.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("Should extract headers, populate context and MDC, and clear in finally")
    void withValidHeaders_shouldPopulateContextAndClearInFinally() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiHeaders.TENANT_ID, tenantId.toString());
        request.addHeader(ApiHeaders.USER_ID, userId.toString());
        request.addHeader(ApiHeaders.ROLES, "ROLE_USER, ROLE_ADMIN");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<ExecutionContext> contextInChain = new AtomicReference<>();
        AtomicReference<String> mdcTenantInChain = new AtomicReference<>();
        AtomicReference<String> mdcUserInChain = new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                contextInChain.set(ExecutionContextHolder.get().orElse(null));
                mdcTenantInChain.set(MDC.get("tenantId"));
                mdcUserInChain.set(MDC.get("userId"));
            }
        };

        filter.doFilter(request, response, filterChain);

        // Verification inside chain
        assertThat(contextInChain.get()).isNotNull();
        assertThat(contextInChain.get().tenantId()).isEqualTo(tenantId);
        assertThat(contextInChain.get().userId()).isEqualTo(userId);
        assertThat(contextInChain.get().roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        assertThat(mdcTenantInChain.get()).isEqualTo(tenantId.toString());
        assertThat(mdcUserInChain.get()).isEqualTo(userId.toString());

        // Cleanup verification (post-filter)
        assertThat(ExecutionContextHolder.get()).isEmpty();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should handle missing optional headers safely")
    void withMissingHeaders_shouldHandleSafely() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<ExecutionContext> contextInChain = new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                contextInChain.set(ExecutionContextHolder.get().orElse(null));
            }
        };

        filter.doFilter(request, response, filterChain);

        assertThat(contextInChain.get()).isNotNull();
        assertThat(contextInChain.get().tenantId()).isNull();
        assertThat(contextInChain.get().userId()).isNull();
        assertThat(contextInChain.get().roles()).isEmpty();

        assertThat(ExecutionContextHolder.get()).isEmpty();
    }

    @Test
    @DisplayName("Should safely ignore malformed UUIDs without throwing")
    void withInvalidUuids_shouldIgnoreGracefully() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiHeaders.TENANT_ID, "not-a-uuid");
        request.addHeader(ApiHeaders.USER_ID, "invalid-user-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<ExecutionContext> contextInChain = new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                contextInChain.set(ExecutionContextHolder.get().orElse(null));
            }
        };

        filter.doFilter(request, response, filterChain);

        assertThat(contextInChain.get()).isNotNull();
        assertThat(contextInChain.get().tenantId()).isNull();
        assertThat(contextInChain.get().userId()).isNull();
    }

    @Test
    @DisplayName("Should always clear context and MDC even when filter chain throws an exception")
    void whenFilterChainThrows_shouldAlwaysClear() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new RuntimeException("Simulated chain failure");
            }
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated chain failure");

        assertThat(ExecutionContextHolder.get()).isEmpty();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }
}