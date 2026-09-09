package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.exception.ConflictException;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.domain.exception.ResourceNotFoundException;
import com.template.api.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handlerWithMasking = new GlobalExceptionHandler(true);
    private final GlobalExceptionHandler handlerWithoutMasking = new GlobalExceptionHandler(false);

    @Nested
    @DisplayName("1. Manejo de BaseException y Subclases de Negocio")
    class BaseExceptionTests {

        @Test
        @DisplayName("Debe transformar ResourceNotFoundException a ErrorResponse 404")
        void shouldHandleResourceNotFoundException() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Customer with ID 123 not found");

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().code()).isEqualTo(CommonError.RESOURCE_NOT_FOUND.code());
            assertThat(response.getBody().detail()).isEqualTo("Customer with ID 123 not found");
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        @DisplayName("Debe transformar ConflictException a ErrorResponse 409")
        void shouldHandleConflictException() {
            ConflictException ex = new ConflictException("Email already registered");

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().code()).isEqualTo(CommonError.RESOURCE_ALREADY_EXISTS.code());
            assertThat(response.getBody().detail()).isEqualTo("Email already registered");
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        @DisplayName("Debe transformar ValidationException a ErrorResponse 400")
        void shouldHandleValidationException() {
            ValidationException ex = new ValidationException("Invalid price amount");

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().code()).isEqualTo(CommonError.VALIDATION_ERROR.code());
            assertThat(response.getBody().detail()).isEqualTo("Invalid price amount");
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        @DisplayName("Debe enmascarar excepciones de categoría INTERNAL si maskInternalDetails es true")
        void shouldMaskInternalDetailsWhenEnabled() {
            InfrastructureException ex = new InfrastructureException(
                    CommonError.INTERNAL_ERROR,
                    "Database connection pool timeout on replica-01"
            );

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response.getBody().detail()).isEqualTo(GlobalExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("No debe enmascarar excepciones de categoría INTERNAL si maskInternalDetails es false")
        void shouldNotMaskInternalDetailsWhenDisabled() {
            InfrastructureException ex = new InfrastructureException(
                    CommonError.INTERNAL_ERROR,
                    "Database connection pool timeout on replica-01"
            );

            ResponseEntity<ErrorResponse> response = handlerWithoutMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response.getBody().detail()).isEqualTo("Database connection pool timeout on replica-01");
        }
    }

    @Nested
    @DisplayName("2. Manejo de Bean Validation (@Valid)")
    class BeanValidationTests {

        @Test
        @DisplayName("Debe transformar MethodArgumentNotValidException a ErrorResponse 400 con lista de errores de campo")
        void shouldHandleMethodArgumentNotValidException() throws Exception {
            Method dummyMethod = DummyController.class.getDeclaredMethod("dummyMethod", String.class);
            MethodParameter parameter = new MethodParameter(dummyMethod, 0);

            DummyDto target = new DummyDto(null, -5);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyDto");
            bindingResult.addError(new FieldError("dummyDto", "email", null, false, null, null, "Email is mandatory"));
            bindingResult.addError(new FieldError("dummyDto", "amount", -5, false, null, null, "Amount must be positive"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

            ResponseEntity<Object> response = handlerWithMasking.handleMethodArgumentNotValid(
                    ex,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    null
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

            ErrorResponse errorResponse = (ErrorResponse) response.getBody();
            assertThat(errorResponse.status()).isEqualTo(400);
            assertThat(errorResponse.code()).isEqualTo(CommonError.VALIDATION_ERROR.code());
            assertThat(errorResponse.detail()).isEqualTo(GlobalExceptionHandler.VALIDATION_FAILED_MESSAGE);
            assertThat(errorResponse.errors()).hasSize(2);

            ValidationErrorDetail error1 = errorResponse.errors().get(0);
            assertThat(error1.field()).isEqualTo("email");
            assertThat(error1.message()).isEqualTo("Email is mandatory");
            assertThat(error1.rejectedValue()).isNull();

            ValidationErrorDetail error2 = errorResponse.errors().get(1);
            assertThat(error2.field()).isEqualTo("amount");
            assertThat(error2.message()).isEqualTo("Amount must be positive");
            assertThat(error2.rejectedValue()).isEqualTo(-5);
        }
    }

    @Nested
    @DisplayName("3. Fallback Catch-All (Throwable / Errores imprevistos)")
    class CatchAllTests {

        @Test
        @DisplayName("Debe interceptar NullPointerException imprevisto y retornar 500 enmascarado")
        void shouldHandleUnexpectedThrowableWithMasking() {
            NullPointerException ex = new NullPointerException("Null pointer at Line 42");

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleUnhandledException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response.getBody().detail()).isEqualTo(GlobalExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE);
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        @DisplayName("Debe interceptar Throwable y revelar mensaje solo si maskInternalDetails es false")
        void shouldHandleUnexpectedThrowableWithoutMasking() {
            NullPointerException ex = new NullPointerException("Null pointer at Line 42");

            ResponseEntity<ErrorResponse> response = handlerWithoutMasking.handleUnhandledException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response.getBody().detail()).isEqualTo("Null pointer at Line 42");
        }
    }

    @Nested
    @DisplayName("4. Sobrescritura de Framework Handler (createResponseEntity)")
    class FrameworkHandlerTests {

        @Test
        @DisplayName("Debe interceptar ProblemDetail de Spring y adaptarlo a ErrorResponse")
        void shouldConvertProblemDetailToErrorResponse() {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "No endpoint GET /api/v1/unknown found"
            );

            ResponseEntity<Object> response = handlerWithMasking.createResponseEntity(
                    problem,
                    new HttpHeaders(),
                    HttpStatusCode.valueOf(404),
                    null
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

            ErrorResponse errorResponse = (ErrorResponse) response.getBody();
            assertThat(errorResponse.status()).isEqualTo(404);
            assertThat(errorResponse.code()).isEqualTo(CommonError.RESOURCE_NOT_FOUND.code());
            assertThat(errorResponse.detail()).isEqualTo("No endpoint GET /api/v1/unknown found");
            assertThat(errorResponse.errors()).isEmpty();
        }
    }

    // Helper classes for reflection
    private static class DummyController {
        @SuppressWarnings("unused")
        void dummyMethod(String input) {}
    }

    private record DummyDto(String email, Integer amount) {}
}
