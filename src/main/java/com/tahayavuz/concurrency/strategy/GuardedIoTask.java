package com.tahayavuz.concurrency.strategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kendi kilidini tutarken I/O bekleyen bir görev.
 * <p>
 * Her görevin <b>kendi</b> kilit nesnesi vardır; yani görevler birbirini beklemez.
 * Buna rağmen {@code synchronized} kullanan sürüm, Java 21'de dramatik biçimde yavaştır.
 *
 * <h2>Sebep: carrier thread pinning</h2>
 * Virtual thread bir I/O'da bloke olduğunda normalde taşıyıcı (carrier) platform
 * thread'inden ayrılır ve taşıyıcı başka bir virtual thread'e verilir. Ancak
 * {@code synchronized} bloğunun içindeyken bu ayrılma yapılamaz — monitör, taşıyıcı
 * thread'e bağlıdır. Virtual thread taşıyıcıya <em>çivilenir</em> (pinned) ve o taşıyıcı
 * bekleme boyunca meşgul kalır.
 * <p>
 * Sonuç: eşzamanlılık, virtual thread sayısıyla değil <b>taşıyıcı havuzunun boyutuyla</b>
 * (varsayılan: çekirdek sayısı) sınırlanır. Virtual thread'e geçmenin tüm kazancı gider.
 * <p>
 * {@link ReentrantLock} aynı korumayı sağlar ama monitöre bağlı değildir; virtual thread
 * kilidi tutarken de taşıyıcıdan ayrılabilir.
 *
 * <h2>JDK sürümü notu</h2>
 * Bu davranış <b>Java 21</b> içindir. JDK 24 ile gelen JEP 491, {@code synchronized}
 * içindeki bloklamanın da artık pinning yapmamasını sağlar. Yani buradaki fark yeni
 * JDK'larda kapanır; ölçüm, üzerinde çalıştığı JDK'ya bağlıdır.
 */
public final class GuardedIoTask {

    private GuardedIoTask() {
    }

    /** {@code synchronized} ile korunur — Java 21'de taşıyıcıyı çivileyecek. */
    public static long withSynchronized(Object lock, int seed, long latencyMillis) {
        synchronized (lock) {
            sleep(latencyMillis);
            return seed;
        }
    }

    /** {@link ReentrantLock} ile korunur — taşıyıcı serbest bırakılabilir. */
    public static long withReentrantLock(ReentrantLock lock, int seed, long latencyMillis) {
        lock.lock();
        try {
            sleep(latencyMillis);
            return seed;
        } finally {
            lock.unlock();
        }
    }

    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Görev kesildi", e);
        }
    }
}
