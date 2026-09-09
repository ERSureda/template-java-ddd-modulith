package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

import java.io.Serial;
import java.util.Objects;

/**
 * Raíz jerárquica de todas las excepciones del sistema.
 * <p>
 * Diseñada para ofrecer alto rendimiento en la JVM omitiendo la captura de stack trace
 * cuando la {@link ErrorCategory} no requiere diagnósticos forenses
 * (e.g. errores de negocio o validación de usuario).
 * <p>
 * Es completamente inmutable y segura para concurrencia.
 */
public abstract class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final ErrorCategory category;

    protected BaseException(
            ErrorCode errorCode,
            ErrorCategory category,
            String message,
            Throwable cause
    ) {
        super(
                resolveMessage(errorCode, message),
                cause,
                /* enableSuppression = */ true,
                /* writableStackTrace = */ capturesDiagnostics(category)
        );

        this.errorCode = errorCode;
        this.category = category;
    }

    protected BaseException(
            ErrorCode errorCode,
            ErrorCategory category,
            String message
    ) {
        this(errorCode, category, message, null);
    }

    private static boolean capturesDiagnostics(ErrorCategory category) {
        return Objects.requireNonNull(category, "category")
                .capturesDiagnostics();
    }

    private static String resolveMessage(
            ErrorCode errorCode,
            String message
    ) {
        if (message != null && !message.isBlank()) {
            return message;
        }

        return Objects.requireNonNull(errorCode, "errorCode")
                .code();
    }

    public final ErrorCode getErrorCode() {
        return errorCode;
    }

    public final ErrorCategory getCategory() {
        return category;
    }
}