package com.template.api.shared.infrastructure.adapter.in.web;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// TODO: Mejorar y optimizar.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROPERTY_ERROR_CODE = "errorCode";
    private static final String PROPERTY_TIMESTAMP = "timestamp";
    private static final String PROPERTY_VIOLATIONS = "violations";
    private static final String URN_TYPE_PREFIX = "urn:error-type:";

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException ex) {
        ErrorCategory category = ex.getCategory();
        HttpStatus status = HttpErrorCategoryMapper.toHttpStatus(category);

        // Logging inteligente basado en la directiva de la JVM:
        // Si capturesDiagnostics() == false -> 1 línea limpia en WARN sin stack trace.
        // Si capturesDiagnostics() == true  -> Stack trace completo en ERROR.
        if (category.capturesDiagnostics()) {
            log.error("Fallo técnico de infraestructura o sistema [code={}]: {}",
                    ex.getErrorCode().code(), ex.getMessage(), ex);
        } else {
            log.warn("Fallo controlado de negocio/cliente [code={}]: {}",
                    ex.getErrorCode().code(), ex.getMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        enrichProblemDetail(problem, ex.getErrorCode().code(), category.name());

        return ResponseEntity.status(status).body(problem);
    }

    // =========================================================================
    // 2. HOMOLOGACIÓN DE ERRORS DE BEAN VALIDATION (@Valid en DTOs)
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Solicitud HTTP rechazada por Bean Validation: {} errores de campo detectados",
                ex.getBindingResult().getFieldErrorCount());

        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene campos inválidos o ausentes"
        );
        enrichProblemDetail(problem, CommonError.VALIDATION_ERROR.code(), ErrorCategory.VALIDATION.name());
        problem.setProperty(PROPERTY_VIOLATIONS, violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // =========================================================================
    // 3. FALLBACK CATCH-ALL (Cualquier error inesperado de la JVM / Framework)
    // =========================================================================

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ProblemDetail> handleUnhandledException(Throwable ex) {
        // Al ser un error imprevisto (NPE, OutOfMemory, Bug), SIEMPRE se registra con stack trace completo
        log.error("Excepción no controlada interceptada en capa web", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno e inesperado en el servidor"
        );
        enrichProblemDetail(
                problem,
                CommonError.INTERNAL_ERROR.code(),
                ErrorCategory.INTERNAL.name()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE ENRIQUECIMIENTO
    // =========================================================================

    private void enrichProblemDetail(ProblemDetail problem, String errorCode, String title) {
        problem.setTitle(title);
        problem.setType(URI.create(URN_TYPE_PREFIX + errorCode.toLowerCase().replace('_', '-')));
        problem.setProperty(PROPERTY_ERROR_CODE, errorCode);
        problem.setProperty(PROPERTY_TIMESTAMP, Instant.now());
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Campo inválido",
                "rejectedValue", String.valueOf(fieldError.getRejectedValue())
        );
    }
}
