package com.tahayavuz.concurrency.strategy;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * {@code IntStream.parallel()} — ortak ForkJoinPool üzerinde çalışır.
 * <p>
 * CPU-bound iş için tasarlanmıştır ve orada iyidir: havuz boyutu çekirdek sayısı kadardır.
 * <p>
 * <b>Tuzak:</b> I/O bekleyen işte kullanmak, JVM genelinde <em>paylaşılan</em> ortak
 * havuzu bloke eder. Aynı uygulamadaki alakasız bir {@code parallelStream()} çağrısı da
 * bu yüzden yavaşlar. Bekleyen iş için burası doğru yer değildir.
 */
public final class ParallelStreamStrategy implements ExecutionStrategy {

    @Override
    public List<Long> execute(int taskCount, IntFunction<Long> task) {
        return IntStream.range(0, taskCount)
                .parallel()
                .mapToObj(task::apply)
                .toList();   // toList() karşılaşma sırasını korur
    }

    @Override
    public String name() {
        return "parallelStream";
    }
}
