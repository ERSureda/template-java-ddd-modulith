package com.template.api.shared.infrastructure.web.error;

import com.taxai.api.shared.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler returning lean {@link ErrorResponse} bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final boolean exposeDetails;

    public GlobalExceptionHandler(@Value("${taxai.errors.expose-details:false}") boolean exposeDetails) {
        this.exposeDetails = exposeDetails;
    }

    /** Single handler for the entire domain BaseException hierarchy. */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        HttpStatus status = ErrorCategoryHttpMapper.toHttpStatus(ex.getCategory());
        String detail = exposeDetails ? ex.getMessage() : null;

        Map<String, List<String>> errors = null;
        if (ex instanceof ValidationException ve && !ve.getValidationErrors().isEmpty()) {
            errors = ve.getValidationErrors();
        }

        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof RateLimitExceededException rle) {
            rle.getRetryAfter().ifPresent(d -> headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, d.toSeconds()))));
        } else if (ex instanceof ServiceUnavailableException sue) {
            sue.getRetryAfter().ifPresent(d -> headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, d.toSeconds()))));
        } else if (ex instanceof UnauthorizedException) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        if (status.is5xxServerError()) {
            log.error("Internal domain failure: code={}, status={}", ex.getCode(), status.value(), ex);
        } else {
            log.debug("Domain exception: code={}, status={}", ex.getCode(), status.value());
        }

        ErrorResponse response = ErrorResponse.of(status.value(), ex.getCode(), detail, errors);
        return new ResponseEntity<>(response, headers, status);
    }

    /** Spring standard validation (@Valid) failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                        .add(fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid"));

        String detail = exposeDetails ? "Validation failed for " + ex.getObjectName() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.VALIDATION_ERROR.code(),
                detail,
                errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** Protocol & parsing errors. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String detail = exposeDetails ? ex.getMessage() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.MALFORMED_JSON.code(),
                detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        String detail = exposeDetails ? "Missing header: " + ex.getHeaderName() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.MISSING_HEADER.code(),
                detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String detail = exposeDetails ? "Missing parameter: " + ex.getParameterName() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.MISSING_PARAMETER.code(),
                detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = exposeDetails ? "Type mismatch for parameter: " + ex.getName() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                CommonError.TYPE_MISMATCH.code(),
                detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        String detail = exposeDetails ? "Endpoint not found: " + ex.getResourcePath() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                CommonError.ENDPOINT_NOT_FOUND.code(),
                detail);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String detail = exposeDetails ? "Method " + ex.getMethod() + " not supported" : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                CommonError.METHOD_NOT_ALLOWED.code(),
                detail);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String detail = exposeDetails ? "Media type not supported: " + ex.getContentType() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                CommonError.MEDIA_TYPE_NOT_SUPPORTED.code(),
                detail);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        String detail = exposeDetails ? "Media type not acceptable" : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_ACCEPTABLE.value(),
                CommonError.MEDIA_TYPE_NOT_ACCEPTABLE.code(),
                detail);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
    }

    /** Catch-all fallback for unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex) {
        log.error("Unhandled server exception", ex);
        String detail = exposeDetails ? ex.getMessage() : null;
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                CommonError.INTERNAL_SERVER_ERROR.code(),
                detail);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
