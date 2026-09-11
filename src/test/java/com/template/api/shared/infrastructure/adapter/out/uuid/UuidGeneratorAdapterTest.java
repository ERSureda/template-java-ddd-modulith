package com.template.api.shared.infrastructure.adapter.out.uuid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorAdapterTest {

    private UuidGeneratorAdapter generator;

    @BeforeEach
    void setUp() {
        generator = new UuidGeneratorAdapter();
    }

    @Test
    @DisplayName("Debe generar UUIDs con versión 7 y variante RFC 4122/9562")
    void shouldGenerateValidUuidV7() {
        UUID id = generator.generateId();

        assertThat(id).isNotNull();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2); // 2 representa la variante RFC 4122/9562 (IETF)
    }

    @Test
    @DisplayName("Los identificadores generados secuencialmente deben ser monotónicos crecientes")
    void shouldGenerateMonotonicallyIncreasingUuids() {
        UUID previous = generator.generateId();

        for (int i = 0; i < 1_000; i++) {
            UUID current = generator.generateId();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    @DisplayName("Debe garantizar unicidad absoluta bajo alta concurrencia multihilo")
    void shouldEnsureUniquenessUnderHighConcurrency() throws InterruptedException {
        int threads = 16;
        int operationsPerThread = 2_000;
        int totalExpectedIds = threads * operationsPerThread;

        Set<UUID> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            generatedIds.add(generator.generateId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            finishLatch.await();
        }

        assertThat(generatedIds).hasSize(totalExpectedIds);
    }
}