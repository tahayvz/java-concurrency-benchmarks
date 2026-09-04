package com.tahayavuz.concurrency.workload;

/**
 * Hesaplayan iş: bir raporun satırlarını toplamayı temsil eder.
 * <p>
 * Burada thread hiç bloke olmaz, sürekli CPU kullanır. Bu iş yükünde virtual thread'in
 * sağladığı avantaj ortadan kalkar: çekirdek sayısından fazla thread açmak işi
 * hızlandırmaz, yalnızca bağlam değiştirme (context switch) maliyeti ekler.
 */
public final class CpuBoundWorkload implements Workload {

    private final int iterations;

    public CpuBoundWorkload(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public long compute(int seed) {
        // Basit ama JIT'in tamamen eleyemeyeceği bir hesap: sonuç seed'e bağlı.
        long acc = seed;
        for (int i = 1; i <= iterations; i++) {
            acc += (acc * 31 + i) % 1_000_003;
        }
        return acc;
    }
}
