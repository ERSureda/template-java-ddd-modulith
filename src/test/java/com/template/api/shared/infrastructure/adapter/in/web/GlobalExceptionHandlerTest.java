package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.FieldViolation;
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
        @DisplayName("Debe transformar ValidationException simple a ErrorResponse 400 sin violaciones")
        void shouldHandleSimpleValidationException() {
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
        @DisplayName("Debe transformar ValidationException con múltiples FieldViolation a ErrorResponse con errors poblado")
        void shouldHandleValidationExceptionWithMultipleViolations() {
            List<FieldViolation> violations = List.of(
                    FieldViolation.of("username", "Username already taken"),
                    FieldViolation.of("age", "Age must be at least 18")
            );
            ValidationException ex = new ValidationException("Command validation failed", violations);

            ResponseEntity<ErrorResponse> response = handlerWithMasking.handleBaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errors()).hasSize(2);
            assertThat(response.getBody().errors().get(0).field()).isEqualTo("username");
            assertThat(response.getBody().errors().get(0).message()).isEqualTo("Username already taken");
            assertThat(response.getBody().errors().get(1).field()).isEqualTo("age");
            assertThat(response.getBody().errors().get(1).message()).isEqualTo("Age must be at least 18");
        }

        @Test
        @DisplayName("Debe retornar respuesta singleton enmascarada para excepciones de categoría INTERNAL si maskInternalDetails es true")
        void shouldReturnCachedInternalResponseWhenMaskingEnabled() {
            InfrastructureException ex = new InfrastructureException(
                    CommonError.INTERNAL_ERROR,
                    "Database connection pool timeout on replica-01"
            );

            ResponseEntity<ErrorResponse> response1 = handlerWithMasking.handleBaseException(ex);
            ResponseEntity<ErrorResponse> response2 = handlerWithMasking.handleBaseException(ex);

            assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response1.getBody()).isNotNull();
            assertThat(response1.getBody().status()).isEqualTo(500);
            assertThat(response1.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response1.getBody().detail()).isEqualTo(GlobalExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE);
            // Comprobación de asignación CERO (mismo objeto singleton en memoria)
            assertThat(response1).isSameAs(response2);
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
    @DisplayName("2. Manejo de Bean Validation (@Valid y Sanitización OWASP)")
    class BeanValidationTests {

        @Test
        @DisplayName("Debe transformar MethodArgumentNotValidException sanitizando campos sensibles (passwords/tokens)")
        void shouldHandleMethodArgumentNotValidExceptionWithSanitization() throws Exception {
            Method dummyMethod = DummyController.class.getDeclaredMethod("dummyMethod", String.class);
            MethodParameter parameter = new MethodParameter(dummyMethod, 0);

            DummyDto target = new DummyDto(null, -5, "secretPassword123", "a".repeat(150));
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyDto");
            bindingResult.addError(new FieldError("dummyDto", "email", null, false, null, null, "Email is mandatory"));
            bindingResult.addError(new FieldError("dummyDto", "amount", -5, false, null, null, "Amount must be positive"));
            bindingResult.addError(new FieldError("dummyDto", "password", "secretPassword123", false, null, null, "Password too weak"));
            bindingResult.addError(new FieldError("dummyDto", "biography", "a".repeat(150), false, null, null, "Too long"));

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
            assertThat(errorResponse.errors()).hasSize(4);

            // 1. Campo normal con valor nulo
            ValidationErrorDetail emailError = errorResponse.errors().get(0);
            assertThat(emailError.field()).isEqualTo("email");
            assertThat(emailError.rejectedValue()).isNull();

            // 2. Campo normal con valor
            ValidationErrorDetail amountError = errorResponse.errors().get(1);
            assertThat(amountError.field()).isEqualTo("amount");
            assertThat(amountError.rejectedValue()).isEqualTo(-5);

            // 3. Campo sensible sanitizado por regla OWASP (no expone la contraseña)
            ValidationErrorDetail passwordError = errorResponse.errors().get(2);
            assertThat(passwordError.field()).isEqualTo("password");
            assertThat(passwordError.rejectedValue()).isEqualTo("[PROTECTED]");

            // 4. Cadena larga truncada por regla anti-DoS
            ValidationErrorDetail bioError = errorResponse.errors().get(3);
            assertThat(bioError.field()).isEqualTo("biography");
            assertThat(bioError.rejectedValue().toString()).endsWith("... [truncated]");
        }
    }

    @Nested
    @DisplayName("3. Fallback Catch-All (Throwable / Errores imprevistos)")
    class CatchAllTests {

        @Test
        @DisplayName("Debe interceptar NullPointerException imprevisto y retornar singleton enmascarado (Zero-Allocation)")
        void shouldHandleUnexpectedThrowableWithZeroAllocation() {
            NullPointerException ex1 = new NullPointerException("Null pointer at Line 42");
            NullPointerException ex2 = new NullPointerException("Null pointer at Line 99");

            ResponseEntity<ErrorResponse> response1 = handlerWithMasking.handleUnhandledException(ex1);
            ResponseEntity<ErrorResponse> response2 = handlerWithMasking.handleUnhandledException(ex2);

            assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response1.getBody()).isNotNull();
            assertThat(response1.getBody().status()).isEqualTo(500);
            assertThat(response1.getBody().code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
            assertThat(response1.getBody().detail()).isEqualTo(GlobalExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE);
            assertThat(response1.getBody().errors()).isEmpty();

            // Zero-allocation: devuelven exactamente la misma referencia en memoria
            assertThat(response1).isSameAs(response2);
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

    private record DummyDto(String email, Integer amount, String password, String biography) {}
}
