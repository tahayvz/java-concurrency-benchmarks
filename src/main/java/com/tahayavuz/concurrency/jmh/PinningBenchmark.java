package com.tahayavuz.concurrency.jmh;

import com.tahayavuz.concurrency.strategy.ExecutionStrategy;
import com.tahayavuz.concurrency.strategy.GuardedIoTask;
import com.tahayavuz.concurrency.strategy.VirtualThreadStrategy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * Aynı iş, aynı strateji (virtual threads), tek fark: kilit türü.
 * <p>
 * Her görev kendi kilidini kullanır; çekişme (contention) yoktur. Aradaki fark tamamen
 * pinning'den gelir. Ayrıntılı açıklama: {@link GuardedIoTask}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 3, time = 2)
public class PinningBenchmark {

    private ExecutionStrategy virtualThreads;
    private List<Object> monitors;
    private List<ReentrantLock> locks;

    @Setup
    public void setUp() {
        virtualThreads = new VirtualThreadStrategy();
        monitors = IntStream.range(0, BenchmarkDefaults.TASKS)
                .mapToObj(i -> new Object())
                .toList();
        locks = IntStream.range(0, BenchmarkDefaults.TASKS)
                .mapToObj(i -> new ReentrantLock())
                .toList();
    }

    @TearDown
    public void tearDown() {
        virtualThreads.close();
    }

    @Benchmark
    public long synchronizedBlock() {
        return sum(virtualThreads.execute(BenchmarkDefaults.TASKS, i ->
                GuardedIoTask.withSynchronized(
                        monitors.get(i), i, BenchmarkDefaults.LATENCY_MS)));
    }

    @Benchmark
    public long reentrantLock() {
        return sum(virtualThreads.execute(BenchmarkDefaults.TASKS, i ->
                GuardedIoTask.withReentrantLock(
                        locks.get(i), i, BenchmarkDefaults.LATENCY_MS)));
    }

    private long sum(List<Long> values) {
        long total = 0;
        for (Long value : values) {
            total += value;
        }
        return total;
    }
}
