package com.tahayavuz.concurrency.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** Future listesini sonuç listesine çevirir; hata durumunda ilk hatayı yükseltir. */
final class Futures {

    private Futures() {
    }

    static List<Long> collect(List<Future<Long>> futures) {
        List<Long> results = new ArrayList<>(futures.size());
        for (Future<Long> future : futures) {
            results.add(get(future));
        }
        return results;
    }

    private static Long get(Future<Long> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Görev beklenirken kesildi", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Görev hata ile sonuçlandı", e.getCause());
        }
    }
}
