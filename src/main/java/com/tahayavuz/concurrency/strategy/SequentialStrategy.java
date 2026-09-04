package com.tahayavuz.concurrency.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Görevleri tek thread'de sırayla çalıştırır.
 * <p>
 * Karşılaştırmanın temel çizgisi. Eşzamanlılığın kazandırdığı süreyi görebilmek için
 * kazanmayan bir seçeneğe ihtiyaç vardır; ayrıca görev sayısı azken bu seçenek
 * gerçekten de en hızlısıdır (thread kurma maliyeti yok).
 */
public final class SequentialStrategy implements ExecutionStrategy {

    @Override
    public List<Long> execute(int taskCount, IntFunction<Long> task) {
        List<Long> results = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            results.add(task.apply(i));
        }
        return results;
    }

    @Override
    public String name() {
        return "sequential";
    }
}
