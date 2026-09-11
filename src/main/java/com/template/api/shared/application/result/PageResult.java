package com.template.api.shared.application.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Record inmutable transversal para respuestas paginadas.
 *
 * @param items         Elementos contenidos en la página actual.
 * @param page          Número de página actual (base 0).
 * @param size          Tamaño máximo de elementos por página.
 * @param totalElements Total de elementos disponibles en todas las páginas.
 * @param totalPages    Total de páginas disponibles calculadas.
 * @param <T>           Tipo de los elementos contenidos en la página.
 */
public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        items = (items != null) ? List.copyOf(items) : List.of();
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative: " + page);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero: " + size);
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements cannot be negative: " + totalElements);
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages cannot be negative: " + totalPages);
        }
    }

    /**
     * Crea una instancia de {@link PageResult} calculando automáticamente el total de páginas.
     */
    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
        int calculatedTotalPages = (size > 0 && totalElements > 0)
                ? (int) Math.ceil((double) totalElements / (double) size)
                : 0;
        return new PageResult<>(items, page, size, totalElements, calculatedTotalPages);
    }

    /**
     * Retorna un resultado de página vacío con los parámetros de paginación dados.
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L, 0);
    }

    /**
     * Transforma los elementos de la página mediante una función de mapeo preservando los metadatos de paginación.
     */
    public <U> PageResult<U> map(Function<? super T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        List<U> mappedItems = items.stream().map(mapper).toList();
        return new PageResult<>(mappedItems, page, size, totalElements, totalPages);
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages > 0;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}