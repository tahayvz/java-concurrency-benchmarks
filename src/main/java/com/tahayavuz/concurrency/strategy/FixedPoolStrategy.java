package com.tahayavuz.concurrency.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * Sabit sayıda platform thread'den oluşan havuz.
 * <p>
 * Java 21 öncesinin standart cevabı. Havuz boyutu bir tavandır: I/O bekleyen görevlerde
 * bu tavan doğrudan eşzamanlılık sınırı olur — 200 görev, 16 thread ve 50 ms gecikme
 * varsa toplam süre yaklaşık {@code 200/16 × 50 ms}'tir. Havuzu büyütmek çözüm gibi
 * görünür ama her platform thread'i ayrı bir işletim sistemi thread'i ve ~1 MB yığın
 * demektir; binlerce eşzamanlı istekte bu ölçeklenmez.
 */
public final class FixedPoolStrategy implements ExecutionStrategy {

    private final ExecutorService executor;
    private final int poolSize;

    public FixedPoolStrategy(int poolSize) {
        this.poolSize = poolSize;
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public List<Long> execute(int taskCount, IntFunction<Long> task) {
        List<Future<Long>> futures = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            int index = i;
            futures.add(executor.submit(() -> task.apply(index)));
        }
        return Futures.collect(futures);
    }

    @Override
    public String name() {
        return "fixedPool(" + poolSize + ")";
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
