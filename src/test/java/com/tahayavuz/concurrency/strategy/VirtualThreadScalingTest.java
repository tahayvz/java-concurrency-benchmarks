package com.tahayavuz.concurrency.strategy;

import com.tahayavuz.concurrency.report.ReportAggregator;
import com.tahayavuz.concurrency.workload.IoBoundWorkload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Virtual thread'lerin bekleyen işte ölçeklendiğini davranış olarak doğrular.
 * <p>
 * Bu bir benchmark değil; kesin süre ölçmez. Ölçtüğü şey <b>büyüklük mertebesi</b>:
 * 200 görev × 50 ms iş, 4 platform thread ile en az 2.5 saniye sürmek zorundadır
 * (200/4 × 50 ms), virtual thread ile ise 1 saniyenin altında biter. Eşik, makine
 * hızına duyarlı olmayacak kadar geniş bırakıldı — CI'da kararsız test istemiyoruz.
 */
@DisplayName("Virtual thread ölçeklenmesi")
class VirtualThreadScalingTest {

    private static final int TASKS = 200;
    private static final long LATENCY_MS = 50;

    private Duration timeOf(ExecutionStrategy strategy) {
        try (strategy) {
            ReportAggregator aggregator =
                    new ReportAggregator(strategy, new IoBoundWorkload(LATENCY_MS));
            Instant start = Instant.now();
            aggregator.aggregate(TASKS);
            return Duration.between(start, Instant.now());
        }
    }

    @Test
    @DisplayName("küçük platform havuzu, havuz boyutunun dayattığı alt sınırın altına inemez")
    void fixedPoolShouldBeBoundedByPoolSize() {
        Duration elapsed = timeOf(new FixedPoolStrategy(4));

        // 200 görev / 4 thread = 50 tur × 50 ms = 2500 ms teorik alt sınır
        assertThat(elapsed).isGreaterThan(Duration.ofMillis(2_000));
    }

    @Test
    @DisplayName("virtual thread'ler aynı işi çok daha kısa sürede bitirir")
    void virtualThreadsShouldNotBeBoundedByPoolSize() {
        Duration elapsed = timeOf(new VirtualThreadStrategy());

        assertThat(elapsed).isLessThan(Duration.ofMillis(1_500));
    }
}
