package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String code,
        String detail,
        List<ValidationErrorDetail> errors
) {
    public static ErrorResponse of(int status, String code, String detail, List<ValidationErrorDetail> errors) {
        return new ErrorResponse(status, code, detail, errors);
    }
}
