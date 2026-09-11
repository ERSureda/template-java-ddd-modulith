package com.template.api.shared.infrastructure.adapter.in.web;

public record ValidationErrorDetail(
        String field,
        String message
) {}