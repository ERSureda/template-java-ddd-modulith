package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpErrorCategoryMapperTest {

    @ParameterizedTest
    @EnumSource(ErrorCategory.class)
    @DisplayName("Todos los valores de ErrorCategory deben mapear a un HttpStatus no nulo")
    void shouldMapAllCategoriesToNonNullHttpStatus(ErrorCategory category) {
        HttpStatus status = HttpErrorCategoryMapper.toHttpStatus(category);
        assertThat(status).isNotNull();
    }

    @Test
    @DisplayName("Debe verificar el mapeo semántico exacto para cada categoría")
    void shouldVerifySpecificMappings() {
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.VALIDATION)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.UNAUTHENTICATED)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.FORBIDDEN)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.CONFLICT)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.DOMAIN_RULE)).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.RATE_LIMITED)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.INTERNAL)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.UNAVAILABLE)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(HttpErrorCategoryMapper.toHttpStatus(ErrorCategory.EXTERNAL_SERVICE)).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException si la categoría es null")
    void shouldThrowWhenCategoryIsNull() {
        assertThatThrownBy(() -> HttpErrorCategoryMapper.toHttpStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("category");
    }
}