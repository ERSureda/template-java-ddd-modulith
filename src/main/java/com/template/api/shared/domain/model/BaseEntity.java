package com.template.api.shared.domain.model;

import java.util.Objects;

public abstract class BaseEntity<T> {

    protected T id;

    protected BaseEntity() {}

    protected BaseEntity(T id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public T getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        if (this.id == null || that.id == null) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
