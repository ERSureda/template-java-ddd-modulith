package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.exception.BaseException;
import com.template.api.shared.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada web centralizado para la captura y estandarización de excepciones.
 * <p>
 * Transforma fallos de dominio, validaciones de Bean Validation y excepciones nativas
 * del framework MVC a un modelo homogéneo {@link ErrorResponse} con trazabilidad distribuida.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String GENERIC_INTERNAL_ERROR_MESSAGE =
            "Ha ocurrido un error interno e inesperado en el servidor";

    public static final String VALIDATION_FAILED_MESSAGE =
            "La solicitud contiene campos inválidos o ausentes";

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String CORRELATION_HEADER_NAME = "X-Correlation-Id";

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
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        ErrorCategory category = ex.getCategory();
        HttpStatus status = HttpErrorCategoryMapper.toHttpStatus(category);
        String code = ex.getErrorCode().code();
        String traceId = resolveTraceId(request);

        if (category.capturesDiagnostics()) {
            log.error("Fallo técnico de infraestructura o sistema [traceId={}, code={}]: {}", traceId, code, ex.getMessage(), ex);
        } else {
            log.warn("Fallo controlado de regla de dominio [traceId={}, code={}]: {}", traceId, code, ex.getMessage());
        }

        if (category == ErrorCategory.INTERNAL && maskInternalDetails) {
            return buildMaskedInternalResponse(traceId);
        }

        List<ValidationErrorDetail> violations = (ex instanceof ValidationException ve && !ve.getViolations().isEmpty())
                ? ve.getViolations().stream()
                .map(v -> new ValidationErrorDetail(v.field(), v.message(), null))
                .toList()
                : List.of();

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                code,
                ex.getMessage(),
                traceId,
                violations
        );

        return ResponseEntity.status(status).body(response);
    }

    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        return handleBaseException(ex, null);
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
        String traceId = resolveTraceId(request);
        log.warn("Solicitud rechazada por validación de payload [traceId={}]: {} violaciones detectadas",
                traceId, ex.getBindingResult().getFieldErrorCount());

        List<ValidationErrorDetail> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                VALIDATION_FAILED_MESSAGE,
                traceId,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(response);
    }

    // =========================================================================
    // 3. VALIDACIÓN DE PARÁMETROS (@PathVariable / @RequestParam)
    // =========================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("Violación de restricción en parámetros HTTP [traceId={}]: {}", traceId, ex.getMessage());

        List<ValidationErrorDetail> violations = ex.getConstraintViolations().stream()
                .map(v -> new ValidationErrorDetail(v.getPropertyPath().toString(), v.getMessage(), v.getInvalidValue()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                VALIDATION_FAILED_MESSAGE,
                traceId,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // 4. FALLBACK CATCH-ALL (Excepciones imprevistas no controladas)
    // =========================================================================

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Throwable ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Excepción no controlada interceptada en capa web [traceId={}]", traceId, ex);

        if (maskInternalDetails) {
            return buildMaskedInternalResponse(traceId);
        }

        String detail = ex.getMessage() != null ? ex.getMessage() : GENERIC_INTERNAL_ERROR_MESSAGE;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                CommonError.INTERNAL_ERROR.code(),
                detail,
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    public ResponseEntity<ErrorResponse> handleUnhandledException(Throwable ex) {
        return handleUnhandledException(ex, null);
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

        String traceId = resolveTraceId(request);
        String code = mapStatusCodeToErrorCode(statusCode);
        String detail = (body instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail()
                : statusCode.toString();

        ErrorResponse errorResponse = ErrorResponse.of(
                statusCode.value(),
                code,
                detail,
                traceId
        );

        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private ResponseEntity<ErrorResponse> buildMaskedInternalResponse(String traceId) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                CommonError.INTERNAL_ERROR.code(),
                GENERIC_INTERNAL_ERROR_MESSAGE,
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String resolveTraceId(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return resolveTraceId(servletWebRequest.getRequest());
        }
        return resolveTraceId((HttpServletRequest) null);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }

        if (request != null) {
            String headerTraceId = request.getHeader(CORRELATION_HEADER_NAME);
            if (headerTraceId != null && !headerTraceId.isBlank()) {
                return headerTraceId;
            }
        }

        return UUID.randomUUID().toString();
    }

    private ValidationErrorDetail mapFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Campo inválido";

        return new ValidationErrorDetail(
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