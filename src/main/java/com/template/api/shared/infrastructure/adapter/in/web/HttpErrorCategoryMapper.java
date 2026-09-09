package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.ErrorCategory;
import org.springframework.http.HttpStatus;

import java.util.Objects;

public final class HttpErrorCategoryMapper {

    private HttpErrorCategoryMapper() {}

    public static HttpStatus toHttpStatus(ErrorCategory category) {
        Objects.requireNonNull(category, "category");

        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST; // 400
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED; // 401
            case FORBIDDEN -> HttpStatus.FORBIDDEN; // 403
            case NOT_FOUND -> HttpStatus.NOT_FOUND; // 404
            case CONFLICT -> HttpStatus.CONFLICT; // 409
            case DOMAIN_RULE -> HttpStatus.UNPROCESSABLE_CONTENT; // 422
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS; // 429
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR; // 500
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE; // 503
            case EXTERNAL_SERVICE -> HttpStatus.BAD_GATEWAY; // 502
        };
    }
}
