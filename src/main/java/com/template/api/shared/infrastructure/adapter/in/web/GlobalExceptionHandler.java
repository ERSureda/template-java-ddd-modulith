package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.exception.BaseException;
import com.template.api.shared.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
 * Adaptador de entrada web centralizado para la captura y estandarización de excepciones.
 * <p>
 * Transforma fallos de dominio, validaciones de Bean Validation y excepciones nativas
 * del framework MVC a un modelo homogéneo ultraligero {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String GENERIC_INTERNAL_ERROR_MESSAGE =
            "Ha ocurrido un error interno e inesperado en el servidor";

    public static final String VALIDATION_FAILED_MESSAGE =
            "La solicitud contiene campos inválidos o ausentes";

    private static final ResponseEntity<ErrorResponse> CACHED_INTERNAL_ERROR_RESPONSE =
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.of(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            CommonError.INTERNAL_ERROR.code(),
                            GENERIC_INTERNAL_ERROR_MESSAGE,
                            List.of()));

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
    // 1. EXCEPCIONES DE DOMINIO Y REGLAS DE NEGOCIO (BaseException)
    // =========================================================================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorCategory category = ex.getCategory();
        HttpStatus status = HttpErrorCategoryMapper.toHttpStatus(category);
        String code = ex.getErrorCode().code();

        if (category.capturesDiagnostics()) {
            log.error("Fallo técnico de infraestructura o sistema [code={}]: {}", code, ex.getMessage(), ex);
        } else {
            log.warn("Fallo controlado de regla de dominio [code={}]: {}", code, ex.getMessage());
        }

        if (category == ErrorCategory.INTERNAL && maskInternalDetails) {
            return CACHED_INTERNAL_ERROR_RESPONSE;
        }

        List<ValidationErrorDetail> violations = (ex instanceof ValidationException ve && !ve.getViolations().isEmpty())
                ? ve.getViolations().stream()
                .map(v -> new ValidationErrorDetail(v.field(), v.message()))
                .toList()
                : List.of();

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                code,
                ex.getMessage(),
                violations
        );

        return ResponseEntity.status(status).body(response);
    }

    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        return handleBaseException(ex);
    }

    // =========================================================================
    // 2. VALIDACIÓN DE PAYLOAD HTTP (@Valid / @Validated en @RequestBody)
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Solicitud rechazada por validación de payload: {} violaciones detectadas",
                ex.getBindingResult().getFieldErrorCount());

        List<ValidationErrorDetail> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                VALIDATION_FAILED_MESSAGE,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(response);
    }

    // =========================================================================
    // 3. VALIDACIÓN DE PARÁMETROS (@PathVariable / @RequestParam)
    // =========================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Violación de restricción en parámetros HTTP: {}", ex.getMessage());

        List<ValidationErrorDetail> violations = ex.getConstraintViolations().stream()
                .map(v -> new ValidationErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                VALIDATION_FAILED_MESSAGE,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        return handleConstraintViolation(ex);
    }

    // =========================================================================
    // 4. FALLBACK CATCH-ALL (Excepciones imprevistas no controladas)
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

    public ResponseEntity<ErrorResponse> handleUnhandledException(Throwable ex, HttpServletRequest request) {
        return handleUnhandledException(ex);
    }

    // =========================================================================
    // 5. SOBREESCRITURA CENTRALIZADA DE SPRING MVC (404, 405, 415, Malformed JSON)
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

        ErrorResponse errorResponse = ErrorResponse.of(
                statusCode.value(),
                code,
                detail
        );

        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private ValidationErrorDetail mapFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Campo inválido";

        return new ValidationErrorDetail(fieldError.getField(), message);
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