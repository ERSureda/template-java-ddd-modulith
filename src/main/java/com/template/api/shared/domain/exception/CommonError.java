package com.template.api.shared.domain.exception;

/**
 * Cross-cutting error codes. A module declares its own {@link ErrorCode} enum for anything
 * specific enough to identify a throw site.
 */
public enum CommonError implements ErrorCode {

    // Business
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    DOMAIN_RULE_VIOLATION,
    CONFLICT,
    DUPLICATE_RESOURCE,
    CONCURRENT_MODIFICATION,
    INVALID_COUNTRY_CODE,
    INVALID_CURRENCY,

    // Security
    UNAUTHORIZED,
    FORBIDDEN,
    RATE_LIMIT_EXCEEDED,

    // Protocol
    MALFORMED_JSON,
    MISSING_HEADER,
    MISSING_PARAMETER,
    TYPE_MISMATCH,
    ENDPOINT_NOT_FOUND,
    METHOD_NOT_ALLOWED,
    MEDIA_TYPE_NOT_SUPPORTED,
    MEDIA_TYPE_NOT_ACCEPTABLE,
    PAYLOAD_TOO_LARGE,
    REQUEST_TIMEOUT,

    // Technical
    INFRASTRUCTURE_ERROR,
    SERVICE_UNAVAILABLE,
    EXTERNAL_SERVICE_ERROR,
    INTERNAL_SERVER_ERROR;

    @Override
    public String code() {
        return name();
    }
}
