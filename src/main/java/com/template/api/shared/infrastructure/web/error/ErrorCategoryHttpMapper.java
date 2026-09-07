package com.template.api.shared.infrastructure.web.error;

import com.taxai.api.shared.domain.exception.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * Single source of truth for mapping domain {@link ErrorCategory} to HTTP status.
 */
final class ErrorCategoryHttpMapper {

    private ErrorCategoryHttpMapper() {}

    static HttpStatus toHttpStatus(ErrorCategory category) {
        if (category == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;           // 400
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;     // 401
            case FORBIDDEN -> HttpStatus.FORBIDDEN;              // 403
            case NOT_FOUND -> HttpStatus.NOT_FOUND;              // 404
            case CONFLICT -> HttpStatus.CONFLICT;                // 409
            case DOMAIN_RULE -> HttpStatus.UNPROCESSABLE_CONTENT; // 422
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;   // 429
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;   // 500
            case EXTERNAL_SERVICE -> HttpStatus.BAD_GATEWAY;     // 502
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;  // 503
        };
    }
}
