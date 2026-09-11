package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String detail,
        String traceId,
        List<ValidationErrorDetail> errors
) {
    public static ErrorResponse of(int status, String code, String detail, String traceId, List<ValidationErrorDetail> errors) {
        return new ErrorResponse(status, code, detail, traceId, errors);
    }

    public static ErrorResponse of(int status, String code, String detail, String traceId) {
        return new ErrorResponse(status, code, detail, traceId, List.of());
    }
}