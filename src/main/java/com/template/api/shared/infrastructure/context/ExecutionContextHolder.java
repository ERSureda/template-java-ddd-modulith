package com.template.api.shared.infrastructure.context;

import com.taxai.api.shared.application.context.ExecutionContext;

import java.util.Optional;

/**
 * Request-scoped holder for the current {@link ExecutionContext}, populated by the web filter that
 * rebuilds it from the Gateway headers and cleared once the request completes.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CURRENT = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    public static void set(ExecutionContext context) {
        CURRENT.set(context);
    }

    public static Optional<ExecutionContext> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
