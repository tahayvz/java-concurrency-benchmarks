package com.tahayavuz.concurrency.workload;

import java.util.concurrent.TimeUnit;

/**
 * Bekleyen iş: uzak bir servisten veri çekmeyi temsil eder.
 * <p>
 * Gerçek bir HTTP çağrısı yerine {@link Thread#sleep} kullanılır. Amaç ağı ölçmek değil,
 * <b>bloke olan bir thread'in maliyetini</b> ölçmektir; uyku bunu deterministik olarak
 * yapar. Gerçek çağrı kullanılsaydı ölçüm ağ dalgalanmasıyla gürültülenirdi.
 * <p>
 * Kritik nokta: burada CPU çalışmıyor. Platform thread'i bu süre boyunca işletim sistemi
 * seviyesinde bloke; virtual thread ise taşıyıcı thread'i serbest bırakır. Ölçüm
 * farkının kaynağı budur.
 */
public final class IoBoundWorkload implements Workload {

    private final long latencyMillis;

    public IoBoundWorkload(long latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    @Override
    public long compute(int seed) {
        try {
            TimeUnit.MILLISECONDS.sleep(latencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("İş yükü kesildi", e);
        }
        return seed;
    }
}
