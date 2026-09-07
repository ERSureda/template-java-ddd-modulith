package com.template.api.shared.infrastructure.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Ultra-lean error body for REST API clients.
 *
 * <p>In production, {@code detail} is {@code null} to minimize payload and prevent data leaks.
 * The frontend reproduces localized user messages by mapping {@code code} against its own i18n bundle.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String code,
        String detail,
        Map<String, List<String>> errors
) {

    public static ErrorResponse of(int status, String code) {
        return new ErrorResponse(status, code, null, null);
    }

    public static ErrorResponse of(int status, String code, String detail) {
        return new ErrorResponse(status, code, detail, null);
    }

    public static ErrorResponse of(int status, String code, String detail, Map<String, List<String>> errors) {
        Map<String, List<String>> safeErrors = (errors != null && !errors.isEmpty()) ? errors : null;
        return new ErrorResponse(status, code, detail, safeErrors);
    }
}
