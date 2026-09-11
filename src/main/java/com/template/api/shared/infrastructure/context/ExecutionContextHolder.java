package com.template.api.shared.infrastructure.context;

import com.template.api.shared.application.context.ExecutionContext;

import java.util.Optional;

/**
 * Portador estático desacoplado para el {@link ExecutionContext}.
 * Diseñado para ser seguro frente a la concurrencia masiva de Virtual Threads en Java 25.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CONTEXT = new ThreadLocal<>();

    private ExecutionContextHolder() {
    }

    /**
     * Establece el contexto para el hilo actual. Si el contexto es nulo, limpia el valor almacenado.
     */
    public static void set(ExecutionContext context) {
        if (context == null) {
            clear();
        } else {
            CONTEXT.set(context);
        }
    }

    /**
     * Obtiene opcionalmente el contexto del hilo actual.
     */
    public static Optional<ExecutionContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * Obtiene el contexto actual o lanza {@link IllegalStateException} si no ha sido inicializado.
     */
    public static ExecutionContext getRequired() {
        ExecutionContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("No ExecutionContext found in current thread");
        }
        return context;
    }

    /**
     * Limpia de forma segura el contexto en el hilo actual para evitar fugas de memoria.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}