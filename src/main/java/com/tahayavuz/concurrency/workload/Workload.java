package com.tahayavuz.concurrency.workload;

/**
 * Tek bir raporun üretilmesi sırasında yapılan iş.
 * <p>
 * Stratejileri karşılaştırırken iş yükünün <b>türü</b> sonucu belirler: bekleyen iş ile
 * hesaplayan iş farklı stratejileri ödüllendirir. Bu arayüz, aynı stratejiyi iki farklı
 * iş yükü altında çalıştırıp farkı ölçebilmek için vardır.
 */
public interface Workload {

    /**
     * İşi yapar ve doğrulanabilir bir sonuç üretir.
     *
     * @param seed görevden göreve değişen girdi; sonuç buna bağlıdır, böylece testler
     *             stratejinin işi gerçekten yaptığını (atlamadığını) doğrulayabilir
     */
    long compute(int seed);
}
