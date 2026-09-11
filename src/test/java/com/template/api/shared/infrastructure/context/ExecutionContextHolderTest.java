package com.template.api.shared.infrastructure.context;

import com.template.api.shared.application.context.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionContextHolder Unit Tests")
class ExecutionContextHolderTest {

    @AfterEach
    void tearDown() {
        ExecutionContextHolder.clear();
    }

    @Test
    @DisplayName("Should set and retrieve context on the same thread")
    void setAndGet_shouldReturnContext() {
        ExecutionContext context = ExecutionContext.anonymous("trace-100");

        ExecutionContextHolder.set(context);

        assertThat(ExecutionContextHolder.get()).contains(context);
        assertThat(ExecutionContextHolder.getRequired()).isSameAs(context);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when calling getRequired on empty holder")
    void getRequired_whenEmpty_shouldThrowIllegalStateException() {
        assertThatThrownBy(ExecutionContextHolder::getRequired)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No ExecutionContext found in current thread");
    }

    @Test
    @DisplayName("Should clear context when setting null or calling clear")
    void clear_shouldRemoveContext() {
        ExecutionContext context = ExecutionContext.anonymous("trace-200");

        ExecutionContextHolder.set(context);
        assertThat(ExecutionContextHolder.get()).isPresent();

        ExecutionContextHolder.set(null);
        assertThat(ExecutionContextHolder.get()).isEmpty();

        ExecutionContextHolder.set(context);
        ExecutionContextHolder.clear();
        assertThat(ExecutionContextHolder.get()).isEmpty();
    }

    @Test
    @DisplayName("Should maintain separate contexts across different threads")
    void concurrency_threadsShouldHaveIsolatedContexts() throws InterruptedException {
        ExecutionContext mainContext = ExecutionContext.anonymous("main-thread");
        ExecutionContextHolder.set(mainContext);

        AtomicReference<Optional<ExecutionContext>> threadContext = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            threadContext.set(ExecutionContextHolder.get());
            latch.countDown();
        });
        otherThread.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(threadContext.get()).isEmpty();
        assertThat(ExecutionContextHolder.get()).contains(mainContext);
    }
}