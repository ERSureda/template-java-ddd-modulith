package com.template.api.shared.domain.model;

import com.template.api.shared.domain.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AggregateRoot Unit Tests")
class AggregateRootTest {

    record DummyEvent(UUID eventId, String aggregateId, Instant occurredAt, String eventType) implements DomainEvent {}

    static class TestAggregate extends AggregateRoot<UUID> {
        TestAggregate() {
            super();
        }

        TestAggregate(UUID id, Long version) {
            super(id, version);
        }

        void doAction(String data) {
            registerEvent(new DummyEvent(UUID.randomUUID(), getId() != null ? getId().toString() : "new", Instant.now(), "dummy.event.v1"));
        }
    }

    @Test
    @DisplayName("Should detect if entity is new based on version")
    void isNew_shouldCheckVersion() {
        TestAggregate newAggregate = new TestAggregate();
        assertThat(newAggregate.getVersion()).isNull();

        TestAggregate persisted = new TestAggregate(UUID.randomUUID(), 0L);
        assertThat(persisted.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should lazily handle domain events and clear memory on pull")
    void lazyEvents_andPullDomainEvents() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID(), 1L);

        // Before any action: no events registered and domainEvents is null
        assertThat(aggregate.hasDomainEvents()).isFalse();
        assertThat(aggregate.pullDomainEvents()).isEmpty();

        // Register one event
        aggregate.doAction("test1");
        assertThat(aggregate.hasDomainEvents()).isTrue();

        // Pull events: should return the registered event and reset to null
        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).eventType()).isEqualTo("dummy.event.v1");

        // Subsequent check: empty again and memory released
        assertThat(aggregate.hasDomainEvents()).isFalse();
        assertThat(aggregate.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should throw NullPointerException when registering null event")
    void registerNullEvent_shouldThrow() {
        TestAggregate aggregate = new TestAggregate();
        assertThatThrownBy(() -> aggregate.registerEvent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("EVENT_CANNOT_BE_NULL");
    }
}