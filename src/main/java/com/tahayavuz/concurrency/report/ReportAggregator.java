package com.tahayavuz.concurrency.report;

import com.tahayavuz.concurrency.strategy.ExecutionStrategy;
import com.tahayavuz.concurrency.workload.Workload;

import java.util.List;

/**
 * Bağımsız parçalardan tek bir rapor toplar.
 * <p>
 * Toplayıcı, işin <b>nasıl</b> yürütüldüğünü bilmez; {@link ExecutionStrategy} dışarıdan
 * verilir. Ölçüm bu ayrım sayesinde mümkün: aynı iş, tek satır değiştirilerek farklı
 * stratejilerle çalıştırılıp karşılaştırılabiliyor.
 */
public final class ReportAggregator {

    private final ExecutionStrategy strategy;
    private final Workload workload;

    public ReportAggregator(ExecutionStrategy strategy, Workload workload) {
        this.strategy = strategy;
        this.workload = workload;
    }

    /**
     * {@code partCount} parçayı üretir ve toplamlarını döner.
     *
     * @return parça sonuçlarının toplamı — benchmark'ta sonucun kullanılması, JIT'in
     *         hesabı ölü kod sayıp elemesini engeller
     */
    public long aggregate(int partCount) {
        List<Long> parts = strategy.execute(partCount, workload::compute);

        long total = 0;
        for (Long part : parts) {
            total += part;
        }
        return total;
    }

    /** Parçaları sırasıyla döner; sıralama garantisini doğrulayan testler için. */
    public List<Long> parts(int partCount) {
        return strategy.execute(partCount, workload::compute);
    }
}
