package com.tahayavuz.concurrency.jmh;

/** Benchmark'lar arasında ortak sabitler. */
final class BenchmarkDefaults {

    /** Tek raporun parça sayısı. */
    static final int TASKS = 100;

    /** I/O-bound iş yükünde parça başına bekleme. */
    static final long LATENCY_MS = 10;

    /**
     * CPU-bound iş yükünde parça başına döngü adedi.
     * Tek parça ~1 ms sürecek şekilde seçildi; böylece iki iş yükü kabaca aynı
     * "sıralı toplam süreye" sahip olur ve karşılaştırma adil kalır.
     */
    static final int CPU_ITERATIONS = 60_000;

    private BenchmarkDefaults() {
    }
}
