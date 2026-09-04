package com.tahayavuz.concurrency.strategy;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Bağımsız görevlerin nasıl yürütüleceği.
 * <p>
 * Tüm uygulamalar aynı sözleşmeye uyar: girdi sırasıyla <b>aynı sırada</b> sonuç döner
 * ve her görev tam olarak bir kez çalışır. Bu sayede stratejiler birbirinin yerine
 * konabilir ve karşılaştırma anlamlı olur — testler bu iki kuralı doğrular.
 */
public interface ExecutionStrategy extends AutoCloseable {

    /**
     * {@code taskCount} adet görevi çalıştırır.
     *
     * @param task görev indeksini sonuca çeviren fonksiyon
     * @return sonuçlar, görev indeksi sırasıyla
     */
    List<Long> execute(int taskCount, IntFunction<Long> task);

    /** Strateji adı — benchmark ve rapor çıktılarında kullanılır. */
    String name();

    /** Varsayılan: kapatılacak kaynağı olmayan stratejiler için. */
    @Override
    default void close() {
    }
}
