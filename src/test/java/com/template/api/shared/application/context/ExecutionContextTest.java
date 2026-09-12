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

        ExecutionContext context = new ExecutionContext(tenantId, userId, roles);

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(context.isAuthenticated()).isTrue();
        assertThat(context.hasTenant()).isTrue();
        assertThat(context.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(context.hasRole("ROLE_GUEST")).isFalse();
    }

    @Test
    @DisplayName("Should handle null attributes gracefully")
    void nullAttributes_shouldDefaultSafely() {
        ExecutionContext ctx = new ExecutionContext(null, null, null);
        assertThat(ctx.roles()).isEmpty();
        assertThat(ctx.isAuthenticated()).isFalse();
        assertThat(ctx.hasTenant()).isFalse();
    }

    @Test
    @DisplayName("Should create anonymous contexts with helper factory")
    void anonymous_factory() {
        ExecutionContext anonymous = ExecutionContext.anonymous();
        assertThat(anonymous.isAuthenticated()).isFalse();
        assertThat(anonymous.hasTenant()).isFalse();
        assertThat(anonymous.roles()).isEmpty();
    }

    @Test
    @DisplayName("Should ensure roles set is unmodifiable")
    void roles_shouldBeUnmodifiable() {
        Set<String> mutableRoles = new HashSet<>();
        mutableRoles.add("ROLE_USER");

        ExecutionContext context = new ExecutionContext(null, null, mutableRoles);
        mutableRoles.add("ROLE_ADMIN");

        assertThat(context.roles()).containsExactly("ROLE_USER");
        assertThatThrownBy(() -> context.roles().add("ROLE_OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}