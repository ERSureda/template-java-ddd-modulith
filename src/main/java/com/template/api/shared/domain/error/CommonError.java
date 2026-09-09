package com.template.api.shared.domain.error;

public enum CommonError implements ErrorCode {

    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    DOMAIN_RULE_VIOLATION,
    CONFLICT,
    DUPLICATE_RESOURCE,
    CONCURRENT_MODIFICATION,
    INVALID_COUNTRY_CODE,
    INVALID_CURRENCY,

    UNAUTHORIZED,
    FORBIDDEN,
    RATE_LIMIT_EXCEEDED,

    INFRASTRUCTURE_ERROR,
    SERVICE_UNAVAILABLE,
    EXTERNAL_SERVICE_ERROR,
    INTERNAL_SERVER_ERROR;

    @Override
    public String code() {
        return name();
    }
}
