package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.exception.BaseException;
import com.template.api.shared.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Manejador centralizado y estandarizado de excepciones para la capa Web.
 * <p>
 * Implementa las directivas normativas {@code INP-03} y {@code ADR-03} optimizado
 * para alto rendimiento (zero-allocation en fallos 500 y sanitización de seguridad OWASP).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String GENERIC_INTERNAL_ERROR_MESSAGE =
            "Ha ocurrido un error interno e inesperado en el servidor";

    public static final String VALIDATION_FAILED_MESSAGE =
            "La solicitud contiene campos inválidos o ausentes";

    // Caché estática singleton para asignación CERO de memoria en errores 500 enmascarados
    private static final ResponseEntity<ErrorResponse> CACHED_INTERNAL_ERROR_RESPONSE =
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            CommonError.INTERNAL_ERROR.code(),
                            GENERIC_INTERNAL_ERROR_MESSAGE
                    ));

    private final boolean maskInternalDetails;

    public GlobalExceptionHandler() {
        this(true);
    }

    public GlobalExceptionHandler(
            @Value("${application.errors.mask-internal-details:true}") boolean maskInternalDetails
    ) {
        this.maskInternalDetails = maskInternalDetails;
    }

    // =========================================================================
    // 1. EXCEPCIONES DE DOMINIO Y NEGOCIO (BaseException y subclases)
    // =========================================================================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorCategory category = ex.getCategory();
        HttpStatus status = HttpErrorCategoryMapper.toHttpStatus(category);
        String code = ex.getErrorCode().code();

        // 1.1 Logging asíncrono optimizado
        if (category.capturesDiagnostics()) {
            log.error("Fallo técnico de infraestructura o sistema [code={}]: {}", code, ex.getMessage(), ex);
        } else {
            log.warn("Fallo controlado de negocio/cliente [code={}]: {}", code, ex.getMessage());
        }

        // 1.2 Zero-allocation si es error 500 interno y está enmascarado
        if (category == ErrorCategory.INTERNAL && maskInternalDetails) {
            return CACHED_INTERNAL_ERROR_RESPONSE;
        }

        // 1.3 Si la excepción contiene violaciones de dominio (ValidationException múltiple), las mapea
        List<ValidationErrorDetail> errors = (ex instanceof ValidationException ve && !ve.getViolations().isEmpty())
                ? ve.getViolations().stream()
                        .map(v -> ValidationErrorDetail.of(v.field(), v.message()))
                        .toList()
                : ErrorResponse.NO_ERRORS;

        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), code, ex.getMessage(), errors));
    }

    // =========================================================================
    // 2. ERRORES DE BEAN VALIDATION (@Valid en DTOs y Requests)
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Solicitud HTTP rechazada por validación de campos: {} errores detectados",
                ex.getBindingResult().getFieldErrorCount());

        List<ValidationErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                VALIDATION_FAILED_MESSAGE,
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(response);
    }

    // =========================================================================
    // 3. FALLBACK CATCH-ALL (Errores imprevistos de la JVM / Framework)
    // =========================================================================

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Throwable ex) {
        log.error("Excepción no controlada interceptada en capa web", ex);

        if (maskInternalDetails) {
            return CACHED_INTERNAL_ERROR_RESPONSE;
        }

        String detail = ex.getMessage() != null ? ex.getMessage() : GENERIC_INTERNAL_ERROR_MESSAGE;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                CommonError.INTERNAL_ERROR.code(),
                detail
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // =========================================================================
    // 4. SOBREESCRITURA DE FRAMEWORK HANDLER (Evita fugas de ProblemDetail)
    // =========================================================================

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ErrorResponse) {
            return ResponseEntity.status(statusCode).headers(headers).body(body);
        }

        String code = mapStatusCodeToErrorCode(statusCode);
        String detail = (body instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail()
                : statusCode.toString();

        ErrorResponse errorResponse = ErrorResponse.of(statusCode.value(), code, detail);
        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private ValidationErrorDetail mapFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Campo inválido";

        return ValidationErrorDetail.of(
                fieldError.getField(),
                message,
                fieldError.getRejectedValue()
        );
    }

    private static String mapStatusCodeToErrorCode(HttpStatusCode status) {
        int value = status.value();
        return switch (value) {
            case 400 -> CommonError.VALIDATION_ERROR.code();
            case 401 -> CommonError.UNAUTHENTICATED_ACCESS.code();
            case 403 -> CommonError.ACCESS_DENIED.code();
            case 404 -> CommonError.RESOURCE_NOT_FOUND.code();
            case 409 -> CommonError.RESOURCE_ALREADY_EXISTS.code();
            case 429 -> CommonError.RATE_LIMIT_EXCEEDED.code();
            case 503 -> CommonError.SERVICE_UNAVAILABLE.code();
            case 502 -> CommonError.EXTERNAL_SERVICE_ERROR.code();
            default -> value >= 500 ? CommonError.INTERNAL_ERROR.code() : CommonError.VALIDATION_ERROR.code();
        };
    }
}
