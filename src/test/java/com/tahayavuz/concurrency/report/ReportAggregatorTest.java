package com.tahayavuz.concurrency.report;

import com.tahayavuz.concurrency.strategy.ExecutionStrategy;
import com.tahayavuz.concurrency.strategy.FixedPoolStrategy;
import com.tahayavuz.concurrency.strategy.ParallelStreamStrategy;
import com.tahayavuz.concurrency.strategy.SequentialStrategy;
import com.tahayavuz.concurrency.strategy.VirtualThreadStrategy;
import com.tahayavuz.concurrency.workload.CpuBoundWorkload;
import com.tahayavuz.concurrency.workload.IoBoundWorkload;
import com.tahayavuz.concurrency.workload.Workload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportAggregator")
class ReportAggregatorTest {

    private static final int PARTS = 64;

    private List<Supplier<ExecutionStrategy>> strategies() {
        return List.of(
                SequentialStrategy::new,
                () -> new FixedPoolStrategy(4),
                VirtualThreadStrategy::new,
                ParallelStreamStrategy::new);
    }

    /**
     * Asıl güvence bu: strateji değiştirmek sonucu değiştirmemeli. Değiştiriyorsa
     * ölçtüğümüz şey "aynı işin farklı hızları" değil, farklı işlerdir.
     */
    @Test
    @DisplayName("tüm stratejiler CPU-bound işte aynı sonucu üretir")
    void allStrategiesShouldAgreeOnCpuBoundResult() {
        Workload workload = new CpuBoundWorkload(500);
        List<Long> totals = new ArrayList<>();

        for (Supplier<ExecutionStrategy> factory : strategies()) {
            try (ExecutionStrategy strategy = factory.get()) {
                totals.add(new ReportAggregator(strategy, workload).aggregate(PARTS));
            }
        }

        assertThat(totals).hasSize(4);
        assertThat(totals).containsOnly(totals.get(0));
    }

    @Test
    @DisplayName("tüm stratejiler I/O-bound işte aynı sonucu üretir")
    void allStrategiesShouldAgreeOnIoBoundResult() {
        Workload workload = new IoBoundWorkload(1);
        List<Long> totals = new ArrayList<>();

        for (Supplier<ExecutionStrategy> factory : strategies()) {
            try (ExecutionStrategy strategy = factory.get()) {
                totals.add(new ReportAggregator(strategy, workload).aggregate(PARTS));
            }
        }

        long expected = (long) PARTS * (PARTS - 1) / 2;   // 0+1+...+63
        assertThat(totals).containsOnly(expected);
    }

    @Test
    @DisplayName("parçalar görev sırasını korur")
    void partsShouldKeepOrder() {
        try (ExecutionStrategy strategy = new VirtualThreadStrategy()) {
            List<Long> parts = new ReportAggregator(strategy, new IoBoundWorkload(1)).parts(32);

            assertThat(parts).hasSize(32);
            for (int i = 0; i < 32; i++) {
                assertThat(parts.get(i)).isEqualTo(i);
            }
        }
    }

    @Test
    @DisplayName("parça yoksa toplam sıfırdır")
    void shouldReturnZeroForNoParts() {
        try (ExecutionStrategy strategy = new SequentialStrategy()) {
            assertThat(new ReportAggregator(strategy, new CpuBoundWorkload(10)).aggregate(0))
                    .isZero();
        }
    }
}
