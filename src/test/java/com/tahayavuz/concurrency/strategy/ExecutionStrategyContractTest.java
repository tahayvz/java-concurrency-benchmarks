package com.tahayavuz.concurrency.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Her stratejinin uyması gereken sözleşme.
 * <p>
 * Aynı testler tüm uygulamalara uygulanır. Bir strateji sırayı bozarsa ya da görevi
 * atlarsa, karşılaştırma anlamsızlaşır — bu yüzden performans ölçümünden önce
 * doğruluğun kanıtlanması gerekir.
 */
@DisplayName("ExecutionStrategy sözleşmesi")
class ExecutionStrategyContractTest {

    static Stream<Supplier<ExecutionStrategy>> strategies() {
        return Stream.of(
                SequentialStrategy::new,
                () -> new FixedPoolStrategy(4),
                VirtualThreadStrategy::new,
                ParallelStreamStrategy::new);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    @DisplayName("sonuçlar görev sırasıyla döner")
    void shouldPreserveTaskOrder(Supplier<ExecutionStrategy> factory) {
        try (ExecutionStrategy strategy = factory.get()) {
            List<Long> results = strategy.execute(200, i -> (long) i);

            assertThat(results).hasSize(200);
            for (int i = 0; i < 200; i++) {
                assertThat(results.get(i)).isEqualTo(i);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    @DisplayName("her görev tam olarak bir kez çalışır")
    void shouldRunEachTaskExactlyOnce(Supplier<ExecutionStrategy> factory) {
        try (ExecutionStrategy strategy = factory.get()) {
            ConcurrentHashMap<Integer, AtomicInteger> callsPerIndex = new ConcurrentHashMap<>();

            strategy.execute(500, i -> {
                callsPerIndex.computeIfAbsent(i, k -> new AtomicInteger()).incrementAndGet();
                return (long) i;
            });

            assertThat(callsPerIndex).hasSize(500);
            assertThat(callsPerIndex.values()).allSatisfy(count ->
                    assertThat(count.get()).isEqualTo(1));
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    @DisplayName("boş görev listesi boş sonuç döner")
    void shouldHandleZeroTasks(Supplier<ExecutionStrategy> factory) {
        try (ExecutionStrategy strategy = factory.get()) {
            assertThat(strategy.execute(0, i -> (long) i)).isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    @DisplayName("görevdeki hata çağırana yükseltilir, sessizce yutulmaz")
    void shouldPropagateTaskFailure(Supplier<ExecutionStrategy> factory) {
        try (ExecutionStrategy strategy = factory.get()) {
            assertThatThrownBy(() -> strategy.execute(50, i -> {
                if (i == 17) {
                    throw new IllegalArgumentException("görev 17 bozuk");
                }
                return (long) i;
            })).isInstanceOf(RuntimeException.class);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    @DisplayName("adı boş olamaz")
    void shouldHaveName(Supplier<ExecutionStrategy> factory) {
        try (ExecutionStrategy strategy = factory.get()) {
            assertThat(strategy.name()).isNotBlank();
        }
    }
}
