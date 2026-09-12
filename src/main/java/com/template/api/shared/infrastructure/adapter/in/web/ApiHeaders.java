package com.template.api.shared.infrastructure.adapter.in.web;

/**
 * Contrato de cabeceras HTTP provenientes del API Gateway o proxies perimetrales (SHR-03).
 */
public final class ApiHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String ROLES = "X-Roles";

    private ApiHeaders() {
    }
}