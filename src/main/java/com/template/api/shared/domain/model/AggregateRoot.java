package com.template.api.shared.domain.model;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AggregateRoot<T> extends BaseEntity<T> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    public boolean hasDomainEvents() {
        return !this.domainEvents.isEmpty();
    }

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "EVENT_CANNOT_BE_NULL"));
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> currentEvents = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return currentEvents;
    }
}
