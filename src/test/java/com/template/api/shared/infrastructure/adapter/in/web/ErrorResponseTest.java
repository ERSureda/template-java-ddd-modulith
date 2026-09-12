package com.template.api.shared.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse Unit Tests")
class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should serialize empty errors list in JSON payload when using NON_NULL")
    void shouldSerializeEmptyErrorsList() throws Exception {
        ErrorResponse response = ErrorResponse.of(404, "RESOURCE_NOT_FOUND", "Entity not found");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":404");
        assertThat(json).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
        assertThat(json).contains("\"detail\":\"Entity not found\"");
        assertThat(json).contains("\"errors\":[]");
    }

    @Test
    @DisplayName("Should serialize errors with field violations")
    void shouldSerializeErrorsWithViolations() throws Exception {
        List<ValidationErrorDetail> violations = List.of(
                new ValidationErrorDetail("email", "must be a valid email address"),
                new ValidationErrorDetail("age", "must be greater than 18")
        );
        ErrorResponse response = ErrorResponse.of(400, "VALIDATION_ERROR", "Validation failed", violations);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(json).contains("\"field\":\"email\"");
        assertThat(json).contains("\"field\":\"age\"");
    }
}
