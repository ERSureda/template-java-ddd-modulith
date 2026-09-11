package com.template.api.shared.application.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionContext Unit Tests")
class ExecutionContextTest {

    @Test
    @DisplayName("Should create context with full attributes")
    void fullAttributes_shouldStoreValues() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");
        String correlationId = "corr-12345";

        ExecutionContext context = new ExecutionContext(tenantId, userId, roles, correlationId);

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(context.correlationId()).isEqualTo(correlationId);
        assertThat(context.isAuthenticated()).isTrue();
        assertThat(context.hasTenant()).isTrue();
        assertThat(context.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(context.hasRole("ROLE_GUEST")).isFalse();
    }

    @Test
    @DisplayName("Should autogenerate correlationId if null or blank")
    void autogenerateCorrelationId_whenNullOrBlank() {
        ExecutionContext ctx1 = new ExecutionContext(null, null, null, null);
        assertThat(ctx1.correlationId()).isNotBlank();
        assertThat(ctx1.roles()).isEmpty();
        assertThat(ctx1.isAuthenticated()).isFalse();
        assertThat(ctx1.hasTenant()).isFalse();

        ExecutionContext ctx2 = new ExecutionContext(null, null, null, "   ");
        assertThat(ctx2.correlationId()).isNotBlank();
    }

    @Test
    @DisplayName("Should create anonymous contexts with helper factories")
    void anonymous_factories() {
        ExecutionContext anonymousWithCorr = ExecutionContext.anonymous("my-trace-id");
        assertThat(anonymousWithCorr.correlationId()).isEqualTo("my-trace-id");
        assertThat(anonymousWithCorr.isAuthenticated()).isFalse();
        assertThat(anonymousWithCorr.hasTenant()).isFalse();
        assertThat(anonymousWithCorr.roles()).isEmpty();

        ExecutionContext anonymousAuto = ExecutionContext.anonymous();
        assertThat(anonymousAuto.correlationId()).isNotBlank();
        assertThat(anonymousAuto.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Should ensure roles set is unmodifiable")
    void roles_shouldBeUnmodifiable() {
        Set<String> mutableRoles = new HashSet<>();
        mutableRoles.add("ROLE_USER");

        ExecutionContext context = new ExecutionContext(null, null, mutableRoles, "corr-1");
        mutableRoles.add("ROLE_ADMIN");

        assertThat(context.roles()).containsExactly("ROLE_USER");
        assertThatThrownBy(() -> context.roles().add("ROLE_OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}