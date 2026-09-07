package com.template.api.shared.application.context;

import java.util.UUID;

public record ExecutionContext(
        UUID userId,
        String userRole,
        UUID tenantId,
        String tenantMembershipRole
) {
}
