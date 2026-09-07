package com.template.api.shared.domain.exception;

/**
 * Contract for strongly-typed error codes, implemented by one enum per bounded context.
 * A code must be unique across the whole application: with no stack trace on business
 * failures, it is the only clue about the throw site.
 */
public interface ErrorCode {

    String code();

    default String messageKey() {
        return "error." + code();
    }
}
