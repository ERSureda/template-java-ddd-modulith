package com.template.api.shared.infrastructure.web;

/**
 * Contract for the trusted headers injected by the API Gateway after Gateway Authentication
 * Offloading (PROJECT_SPEC §4.2). The Gateway strips any client-supplied {@code X-User-*} /
 * {@code X-Tenant-*} header before injecting its own, so the backend can trust these verbatim.
 */
public final class ApiHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private ApiHeaders() {}
}
