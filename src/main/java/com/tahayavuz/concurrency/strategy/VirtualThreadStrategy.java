package com.tahayavuz.concurrency.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

/**
 * Her görev için bir virtual thread (Java 21).
 * <p>
 * Virtual thread <b>daha hızlı çalışmaz</b>; daha ucuz bloke olur. Görev bir I/O
 * beklerken JVM onu taşıyıcı platform thread'inden ayırır ve o taşıyıcıya başka bir
 * virtual thread verir. Böylece eşzamanlılık, thread sayısıyla değil bekleyen iş
 * sayısıyla sınırlanır.
 * <p>
 * <b>Dikkat:</b> CPU-bound işte kazanç yoktur — bekleme olmadığı için ayrılacak an da
 * yoktur; iş yine çekirdek sayısı kadar paralel ilerler.
 * <p>
 * Executor her görev için yeni bir virtual thread açar; havuz yoktur ve olmamalıdır.
 * Virtual thread'i havuzda tutmak, ucuz olan kaynağı pahalıymış gibi yönetmektir.
 */
public final class VirtualThreadStrategy implements ExecutionStrategy {

    @Override
    public List<Long> execute(int taskCount, IntFunction<Long> task) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Long>> futures = new ArrayList<>(taskCount);
            for (int i = 0; i < taskCount; i++) {
                int index = i;
                futures.add(executor.submit(() -> task.apply(index)));
            }
            return Futures.collect(futures);
        }
    }

    @Override
    public String name() {
        return "virtualThreads";
    }
}
