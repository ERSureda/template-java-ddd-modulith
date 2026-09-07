package com.template.api.shared.domain.exception;

import java.util.Locale;

/**
 * An integrated third party failed. Maps to HTTP 502 so its failures do not contaminate our own
 * error rate.
 */
public final class ExternalServiceException extends BaseException {

    private static final String UNKNOWN_SERVICE = "unknown";

    private final String service;

    public ExternalServiceException(String service, String message) {
        this(CommonError.EXTERNAL_SERVICE_ERROR, service, message, null);
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        this(CommonError.EXTERNAL_SERVICE_ERROR, service, message, cause);
    }

    public ExternalServiceException(ErrorCode errorCode, String service, String message) {
        this(errorCode, service, message, null);
    }

    public ExternalServiceException(
            ErrorCode errorCode, String service, String message, Throwable cause, Object... messageArgs) {
        super(errorCode, ErrorCategory.EXTERNAL_SERVICE, message, cause, messageArgs);
        this.service = normalize(service);
    }

    /** Normalized provider name for logs and context. Never null. */
    public String getService() {
        return service;
    }

    private static String normalize(String service) {
        return service == null || service.isBlank()
                ? UNKNOWN_SERVICE
                : service.strip().toLowerCase(Locale.ROOT);
    }
}
