package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.exception.BaseException;
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
 * Transforma todas las excepciones de la aplicación al contrato ultraligero {@link ErrorResponse},
 * cumpliendo con la regla normativa {@code INP-03} y la decisión de arquitectura {@code ADR-03}.
 * <p>
 * Características clave:
 * <ul>
 *   <li><b>Rendimiento y coste cero:</b> No genera ni inspecciona stack traces para errores de negocio/cliente.</li>
 *   <li><b>Logging inteligente:</b> Registra en {@code WARN} (1 línea) fallos de validación/cliente, y en {@code ERROR} (con stack trace) fallos imprevistos o de sistema.</li>
 *   <li><b>Seguridad:</b> Enmascara detalles y trazas internas en errores 500 para evitar fugas de información.</li>
 *   <li><b>Homologación total:</b> Sustituye {@link ProblemDetail} de Spring por {@link ErrorResponse} en todos los endpoints.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String GENERIC_INTERNAL_ERROR_MESSAGE =
            "Ha ocurrido un error interno e inesperado en el servidor";

    public static final String VALIDATION_FAILED_MESSAGE =
            "La solicitud contiene campos inválidos o ausentes";

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

        if (category.capturesDiagnostics()) {
            log.error("Fallo técnico de infraestructura o sistema [code={}]: {}", code, ex.getMessage(), ex);
        } else {
            log.warn("Fallo controlado de negocio/cliente [code={}]: {}", code, ex.getMessage());
        }

        String detail = (category == ErrorCategory.INTERNAL && maskInternalDetails)
                ? GENERIC_INTERNAL_ERROR_MESSAGE
                : ex.getMessage();

        ErrorResponse response = ErrorResponse.of(status.value(), code, detail);
        return ResponseEntity.status(status).body(response);
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
    // 3. FALLBACK CATCH-ALL (Cualquier excepción imprevista de la JVM / Framework)
    // =========================================================================

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Throwable ex) {
        log.error("Excepción no controlada interceptada en capa web", ex);

        String detail = maskInternalDetails
                ? GENERIC_INTERNAL_ERROR_MESSAGE
                : (ex.getMessage() != null ? ex.getMessage() : GENERIC_INTERNAL_ERROR_MESSAGE);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                CommonError.INTERNAL_ERROR.code(),
                detail
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // =========================================================================
    // 4. SOBRESCRITURA DE FRAMEWORK HANDLER (Evita fugas de ProblemDetail)
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
        String detail;
        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            detail = problemDetail.getDetail();
        } else {
            detail = statusCode.toString();
        }

        ErrorResponse errorResponse = ErrorResponse.of(statusCode.value(), code, detail);
        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private ValidationErrorDetail mapFieldError(FieldError fieldError) {
        String field = fieldError.getField();
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Campo inválido";
        Object rejectedValue = fieldError.getRejectedValue();

        return new ValidationErrorDetail(field, message, rejectedValue);
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
