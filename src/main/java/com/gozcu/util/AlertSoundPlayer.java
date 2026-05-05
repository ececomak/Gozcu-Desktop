package com.gozcu.util;

/*
 * AlertSoundPlayer.java — NFPA 72 ses deseni oynatıcısı
 * ─────────────────────────────────────────────────────────────────
 * Standart: NFPA 72 (2022) Bölüm 18 & 24 — Sesli Uyarı Sinyalleri
 *
 *   §18.4.4 Temporal-Three (T-3) Deseni:
 *     0.5sn AÇIK → 0.5sn KAPALI → 0.5sn AÇIK → 0.5sn KAPALI
 *     → 0.5sn AÇIK → 1.5sn KAPALI → [tekrar]
 *     (Yangın alarmı için evrensel standart)
 *
 *   §24.6   Tahliye (Evacuation) Tonu:
 *     Sürekli yüksek frekanslı bip (1000 Hz, %100 duty-cycle)
 *     Ekip çağırma / acil tahliye sinyali.
 *
 * Uygulama: Programatik ses üretimi — harici ses dosyası gerektirmez.
 *           javax.sound.sampled (JDK içinde) kullanır → ekstra bağımlılık yok.
 *
 * JavaFX AudioClip notu:
 *   Proje zaten JavaFX kullanıyor; AudioClip ses dosyası (.wav/.mp3) ister.
 *   Bu sınıf javax.sound.sampled ile PURE JAVA olarak ses üretir.
 *   Gerçek ses dosyalarınız varsa AudioClip sürümünü kullanabilirsiniz
 *   (bkz. playFromResource() metodu yorum satırları).
 * ─────────────────────────────────────────────────────────────────
 */

import javax.sound.sampled.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * NFPA 72 §18.4.4 Temporal-3 ve §24.6 Tahliye tonu oynatıcısı.
 *
 * <p>Singleton (thread-safe). Kullanım:
 * <pre>{@code
 *   // Yangın alarmı tonu — Temporal-3
 *   AlertSoundPlayer.getInstance().playFireAlarm();
 *
 *   // Tahliye tonu
 *   AlertSoundPlayer.getInstance().playEvacuation();
 *
 *   // Durdur
 *   AlertSoundPlayer.getInstance().stop();
 * }</pre>
 */
public class AlertSoundPlayer {

    private static final Logger LOG = Logger.getLogger(AlertSoundPlayer.class.getName());

    // ── Singleton ────────────────────────────────────────────────
    private static volatile AlertSoundPlayer instance;

    public static AlertSoundPlayer getInstance() {
        if (instance == null) {
            synchronized (AlertSoundPlayer.class) {
                if (instance == null) instance = new AlertSoundPlayer();
            }
        }
        return instance;
    }

    // ── Ses parametreleri ────────────────────────────────────────

    /** Örnekleme frekansı (Hz) — CD kalitesi */
    private static final float SAMPLE_RATE = 44_100f;

    /**
     * NFPA 72 §18.4.4 Temporal-3 ton frekansı.
     * Standart: 520 ± 10 Hz sinyal
     */
    private static final double FIRE_ALARM_HZ = 520.0;

    /**
     * Tahliye tonu frekansı.
     * NFPA 72 §24.6: genellikle 1000 Hz veya üzeri
     */
    private static final double EVACUATION_HZ = 1000.0;

    /** Ses amplitude (0.0–1.0) */
    private static final double AMPLITUDE = 0.6;

    // ── Çalma durumu ─────────────────────────────────────────────

    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "alert-sound");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> currentTask;

    private AlertSoundPlayer() {}

    // ── Genel API ────────────────────────────────────────────────

    /**
     * NFPA 72 §18.4.4 Temporal-Three deseni ile yangın alarmı çalar.
     * Desen sonsuz döngüde tekrarlanır; {@link #stop()} çağrılana kadar devam eder.
     *
     * <p>Temporal-3 zamanlaması:
     * <pre>
     *   [0.5s ON][0.5s OFF][0.5s ON][0.5s OFF][0.5s ON][1.5s OFF] → tekrar
     * </pre>
     */
    public synchronized void playFireAlarm() {
        stop();
        playing.set(true);
        LOG.info("NFPA 72 §18.4.4 Temporal-3 alarmı başlatıldı.");

        // Temporal-3 deseni: 3.5 saniye periyot
        currentTask = scheduler.scheduleAtFixedRate(() -> {
            if (!playing.get()) return;
            playTemporal3Pattern();
        }, 0, (long) (3_500), TimeUnit.MILLISECONDS);
    }

    /**
     * NFPA 72 §24.6 Tahliye (Evacuation) tonu çalar.
     * Sürekli yüksek bip — ekip çağırma / acil tahliye.
     */
    public synchronized void playEvacuation() {
        stop();
        playing.set(true);
        LOG.info("NFPA 72 §24.6 Tahliye tonu başlatıldı.");

        currentTask = scheduler.scheduleAtFixedRate(() -> {
            if (!playing.get()) return;
            beep(EVACUATION_HZ, 800); // 800ms yüksek bip
        }, 0, 1_000, TimeUnit.MILLISECONDS);
    }

    /**
     * Aktif sesi durdurur.
     */
    public synchronized void stop() {
        playing.set(false);
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    /** @return true → ses şu an çalıyor */
    public boolean isPlaying() { return playing.get(); }

    // ── NFPA 72 Temporal-3 Implementasyonu ───────────────────────

    /**
     * Bir tam Temporal-3 döngüsü oynatır (3.5 sn):
     * 0.5-AÇIK 0.5-KAPALI 0.5-AÇIK 0.5-KAPALI 0.5-AÇIK 1.5-KAPALI
     *
     * <p>NFPA 72 §18.4.4.1 gereği her vuruş 0.5 ± 0.05 sn olmalıdır.
     */
    private void playTemporal3Pattern() {
        try {
            // 3 adet "vuruş", her vuruştan sonra 0.5sn sessizlik
            for (int i = 0; i < 3; i++) {
                if (!playing.get()) return;
                beep(FIRE_ALARM_HZ, 500);  // 500ms ON
                Thread.sleep(500);          // 500ms OFF (sessizlik)
            }
            // Son vuruştan sonra uzun bekleme (1.5sn)
            Thread.sleep(1_000); // (zaten 500ms beklendi, ek 1000ms)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Programatik Ses Üretimi (javax.sound.sampled) ────────────

    /**
     * Verilen frekansta sinüs dalgası üretir ve sistem ses çıkışına gönderir.
     *
     * <p>javax.sound.sampled — harici bağımlılık gerektirmez.
     *
     * @param hz       Ton frekansı (Hz)
     * @param durationMs Süre (milisaniye)
     */
    private void beep(double hz, int durationMs) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                LOG.warning("Ses çıkışı desteklenmiyor (headless mod?)");
                return;
            }

            int totalSamples = (int)(SAMPLE_RATE * durationMs / 1000.0);
            byte[] buf = new byte[totalSamples * 2];

            // Sinüs dalgası örnekleme
            for (int i = 0; i < totalSamples; i++) {
                double angle = 2.0 * Math.PI * hz * i / SAMPLE_RATE;
                short sample = (short)(Short.MAX_VALUE * AMPLITUDE * Math.sin(angle));
                buf[i * 2]     = (byte)(sample & 0xFF);
                buf[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
            }

            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(format);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
            }

        } catch (LineUnavailableException e) {
            LOG.warning("Ses satırı kullanılamıyor: " + e.getMessage());
        } catch (Exception e) {
            LOG.warning("Ses çalma hatası: " + e.getMessage());
        }
    }

    // ── JavaFX AudioClip alternatifi (ses dosyası varsa) ─────────
    //
    // Proje resources/ altında nfpa_t3.wav dosyanız varsa:
    //
    //   private javafx.scene.media.AudioClip clip;
    //
    //   public void playFromResource(String resourcePath) {
    //       URL url = getClass().getResource(resourcePath);
    //       if (url == null) { LOG.severe("Ses dosyası bulunamadı: " + resourcePath); return; }
    //       clip = new javafx.scene.media.AudioClip(url.toExternalForm());
    //       clip.setCycleCount(javafx.scene.media.AudioClip.INDEFINITE);
    //       clip.play();
    //   }
    //
    //   public void stopClip() { if (clip != null) clip.stop(); }
}
