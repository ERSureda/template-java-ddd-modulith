package com.template.api.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseEntity Unit Tests")
class BaseEntityTest {

    static class TestEntity extends BaseEntity<UUID> {
        TestEntity() {
            super();
        }

        TestEntity(UUID id) {
            super(id);
        }

        void setId(UUID id) {
            this.id = id;
        }
    }

    @Test
    @DisplayName("Should be equal when comparing the same instance")
    void sameInstance_shouldBeEqual() {
        TestEntity entity = new TestEntity(UUID.randomUUID());
        assertThat(entity.equals(entity)).isTrue();
    }

    @Test
    @DisplayName("Should be equal when IDs match")
    void sameId_shouldBeEqual() {
        UUID id = UUID.randomUUID();
        TestEntity e1 = new TestEntity(id);
        TestEntity e2 = new TestEntity(id);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when IDs differ")
    void differentId_shouldNotBeEqual() {
        TestEntity e1 = new TestEntity(UUID.randomUUID());
        TestEntity e2 = new TestEntity(UUID.randomUUID());

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("Should not be equal when either or both IDs are null")
    void nullId_shouldNotBeEqual() {
        TestEntity transient1 = new TestEntity();
        TestEntity transient2 = new TestEntity();
        TestEntity persisted = new TestEntity(UUID.randomUUID());

        assertThat(transient1).isNotEqualTo(transient2);
        assertThat(transient1).isNotEqualTo(persisted);
        assertThat(persisted).isNotEqualTo(transient1);
        assertThat(transient1.hashCode()).isNotZero();
    }

    @Test
    @DisplayName("Should throw NullPointerException when constructing with null ID")
    void nullIdConstructor_shouldThrow() {
        assertThatThrownBy(() -> new TestEntity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id cannot be null");
    }
}