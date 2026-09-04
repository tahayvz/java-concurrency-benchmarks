package com.tahayavuz.concurrency.jmh;

import com.tahayavuz.concurrency.report.ReportAggregator;
import com.tahayavuz.concurrency.strategy.ExecutionStrategy;
import com.tahayavuz.concurrency.strategy.FixedPoolStrategy;
import com.tahayavuz.concurrency.strategy.ParallelStreamStrategy;
import com.tahayavuz.concurrency.strategy.SequentialStrategy;
import com.tahayavuz.concurrency.strategy.VirtualThreadStrategy;
import com.tahayavuz.concurrency.workload.IoBoundWorkload;
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

import java.util.concurrent.TimeUnit;

/**
 * Bekleyen iş: 100 parça × 10 ms gecikme.
 * <p>
 * Beklenen sıralama: virtual threads ≪ parallelStream ≈ fixedPool ≪ sequential.
 * Virtual thread'in kazanmasının sebebi hız değil, bekleyen görevin taşıyıcı thread'i
 * işgal etmemesidir.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 3, time = 2)
public class IoBoundBenchmark {

    private IoBoundWorkload workload;
    private ExecutionStrategy sequential;
    private ExecutionStrategy fixedPool;
    private ExecutionStrategy virtualThreads;
    private ExecutionStrategy parallelStream;

    @Setup
    public void setUp() {
        workload = new IoBoundWorkload(BenchmarkDefaults.LATENCY_MS);
        sequential = new SequentialStrategy();
        fixedPool = new FixedPoolStrategy(16);
        virtualThreads = new VirtualThreadStrategy();
        parallelStream = new ParallelStreamStrategy();
    }

    @TearDown
    public void tearDown() {
        sequential.close();
        fixedPool.close();
        virtualThreads.close();
        parallelStream.close();
    }

    private long run(ExecutionStrategy strategy) {
        return new ReportAggregator(strategy, workload).aggregate(BenchmarkDefaults.TASKS);
    }

    @Benchmark
    public long sequential() {
        return run(sequential);
    }

    @Benchmark
    public long fixedPool16() {
        return run(fixedPool);
    }

    @Benchmark
    public long virtualThreads() {
        return run(virtualThreads);
    }

    @Benchmark
    public long parallelStream() {
        return run(parallelStream);
    }
}
