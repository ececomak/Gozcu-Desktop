package com.gozcu.inference;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * webcam-capture ile kamera akışını arka planda okur,
 * ONNX inference çalıştırır ve sonuçları JavaFX thread'ine iletir.
 *
 * Kamera kaynakları:
 *   "0" → ilk kamera (genellikle dahili)
 *   "1" → ikinci kamera (harici USB webcam)
 */
public class CameraWorker {

    /** Her kaç frame'de bir inference yapılsın (FPS koruması). 10 = saniyede ~3 kez inference */
    private static final int INFER_EVERY = 10;

    private final SmokeDetector detector;

    private Consumer<Image>                 onFrame;
    private Consumer<List<DetectionResult>> onDetection;
    private Consumer<String>                onError;
    private Consumer<Double>                onFps;

    private volatile float     confThreshold = 0.4f;
    private final AtomicBoolean running      = new AtomicBoolean(false);
    private ExecutorService     executor;

    public CameraWorker(SmokeDetector detector) {
        this.detector = detector;
    }

    // ── Callback setters ─────────────────────────────────────────────────────

    public void setOnFrame(Consumer<Image> cb)                    { onFrame      = cb; }
    public void setOnDetection(Consumer<List<DetectionResult>> cb){ onDetection  = cb; }
    public void setOnError(Consumer<String> cb)                   { onError      = cb; }
    public void setOnFps(Consumer<Double> cb)                     { onFps        = cb; }
    public void setConfThreshold(float t)                         { confThreshold = t; }

    // ── Kontrol ──────────────────────────────────────────────────────────────

    /**
     * @param source "0" veya "1" (kamera index)
     */
    public void start(String source) {
        if (running.get()) return;
        running.set(true);

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "camera-worker");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> cameraLoop(source));
    }

    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public boolean isRunning() { return running.get(); }

    // ── Ana döngü ────────────────────────────────────────────────────────────

    private void cameraLoop(String source) {
        Webcam webcam = null;
        try {
            webcam = openWebcam(source);
            if (webcam == null) throw new RuntimeException("Kamera açılamadı: " + source);

            int frameIdx = 0;
            List<DetectionResult> lastDets = Collections.emptyList();

            long fpsTimestamp = System.currentTimeMillis();
            int  fpsCnt       = 0;

            while (running.get()) {
                BufferedImage raw = webcam.getImage();
                if (raw == null) continue;

                frameIdx++;
                fpsCnt++;

                // Inference — her INFER_EVERY frame'de bir
                if (frameIdx % INFER_EVERY == 0) {
                    try {
                        lastDets = detector.detect(raw, confThreshold);
                    } catch (Exception ex) {
                        System.err.println("Inference hata: " + ex.getMessage());
                        lastDets = Collections.emptyList();
                    }
                }

                // Bounding box'ları frame üzerine çiz
                BufferedImage annotated = detector.annotate(raw, lastDets);

                // JavaFX Image'e çevir ve UI'ya gönder
                Image fxImg = toFXImage(annotated);
                List<DetectionResult> detsSnap = lastDets;

                Platform.runLater(() -> {
                    if (onFrame     != null) onFrame.accept(fxImg);
                    if (onDetection != null) onDetection.accept(detsSnap);
                });

                // FPS hesapla — saniyede bir güncelle
                long now = System.currentTimeMillis();
                if (now - fpsTimestamp >= 1000) {
                    double fps = fpsCnt * 1000.0 / (now - fpsTimestamp);
                    if (onFps != null) Platform.runLater(() -> onFps.accept(fps));
                    fpsCnt       = 0;
                    fpsTimestamp = now;
                }
            }
        } catch (Exception e) {
            System.err.println("CameraWorker hata: " + e.getMessage());
            if (onError != null) Platform.runLater(() -> onError.accept(e.getMessage()));
        } finally {
            if (webcam != null) {
                try { webcam.close(); } catch (Exception ignored) {}
            }
            System.out.println("Kamera kapatıldı.");
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private Webcam openWebcam(String source) throws Exception {
        int index = 0;
        try { index = Integer.parseInt(source.trim()); }
        catch (NumberFormatException ignored) {}

        List<Webcam> webcams = Webcam.getWebcams();
        System.out.println("Bulunan kameralar: " + webcams.size());
        webcams.forEach(w -> System.out.println("  - " + w.getName()));

        if (index >= webcams.size()) {
            throw new RuntimeException("Kamera index " + index + " bulunamadı. Mevcut: " + webcams.size());
        }

        Webcam webcam = webcams.get(index);
        webcam.setViewSize(WebcamResolution.VGA.getSize()); // 640×480
        webcam.open();
        System.out.println("Açıldı: " + webcam.getName());
        return webcam;
    }

    /** BufferedImage → JavaFX Image (SwingFXUtils gerektirmez) */
    private Image toFXImage(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        WritableImage wi = new WritableImage(w, h);
        wi.getPixelWriter().setPixels(0, 0, w, h,
                PixelFormat.getIntArgbInstance(), argb, 0, w);
        return wi;
    }
}
