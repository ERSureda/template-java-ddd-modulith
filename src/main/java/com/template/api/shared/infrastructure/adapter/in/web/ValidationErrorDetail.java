package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        String field,
        String message,
        Object rejectedValue
) {}