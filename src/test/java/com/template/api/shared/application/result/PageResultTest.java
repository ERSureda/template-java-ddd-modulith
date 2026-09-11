package com.template.api.shared.application.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PageResult Unit Tests")
class PageResultTest {

    @Test
    @DisplayName("Should correctly calculate totalPages using of factory method")
    void of_withValidData_shouldCalculateTotalPagesCorrectly() {
        // 25 elements, size 10 -> 3 pages
        PageResult<String> page1 = PageResult.of(List.of("a", "b"), 0, 10, 25);
        assertThat(page1.totalPages()).isEqualTo(3);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();
        assertThat(page1.isEmpty()).isFalse();

        // 20 elements, size 10 -> 2 pages
        PageResult<String> page2 = PageResult.of(List.of("a"), 1, 10, 20);
        assertThat(page2.totalPages()).isEqualTo(2);
        assertThat(page2.hasNext()).isFalse();
        assertThat(page2.hasPrevious()).isTrue();

        // 0 elements, size 10 -> 0 pages
        PageResult<String> page3 = PageResult.of(List.of(), 0, 10, 0);
        assertThat(page3.totalPages()).isEqualTo(0);
        assertThat(page3.hasNext()).isFalse();
        assertThat(page3.hasPrevious()).isFalse();
        assertThat(page3.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should create an empty PageResult")
    void empty_shouldReturnEmptyPageResult() {
        PageResult<Integer> empty = PageResult.empty(0, 20);

        assertThat(empty.items()).isEmpty();
        assertThat(empty.page()).isEqualTo(0);
        assertThat(empty.size()).isEqualTo(20);
        assertThat(empty.totalElements()).isEqualTo(0L);
        assertThat(empty.totalPages()).isEqualTo(0);
        assertThat(empty.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should ensure items list is unmodifiable and null items default to empty list")
    void immutability_itemsShouldBeUnmodifiable() {
        List<String> mutableList = new ArrayList<>();
        mutableList.add("item1");

        PageResult<String> result = new PageResult<>(mutableList, 0, 10, 1, 1);
        mutableList.add("item2");

        assertThat(result.items()).containsExactly("item1");
        assertThatThrownBy(() -> result.items().add("item3"))
                .isInstanceOf(UnsupportedOperationException.class);

        PageResult<String> nullItemsResult = new PageResult<>(null, 0, 10, 0, 0);
        assertThat(nullItemsResult.items()).isEmpty();
    }

    @Test
    @DisplayName("Should map items preserving pagination metadata")
    void map_shouldTransformElementsPreservingMetadata() {
        PageResult<Integer> numbers = PageResult.of(List.of(1, 2, 3), 1, 3, 10);
        PageResult<String> mapped = numbers.map(n -> "Value: " + n);

        assertThat(mapped.items()).containsExactly("Value: 1", "Value: 2", "Value: 3");
        assertThat(mapped.page()).isEqualTo(numbers.page());
        assertThat(mapped.size()).isEqualTo(numbers.size());
        assertThat(mapped.totalElements()).isEqualTo(numbers.totalElements());
        assertThat(mapped.totalPages()).isEqualTo(numbers.totalPages());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when inputs are invalid")
    void validation_invalidInputs_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page index cannot be negative");

        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be greater than zero");

        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total elements cannot be negative");

        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total pages cannot be negative");
    }
}