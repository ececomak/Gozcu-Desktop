package com.gozcu.inference;

import com.gozcu.model.Camera;
import com.gozcu.util.AppSettings;
import javafx.scene.image.Image;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Tüm kamera worker'larını ve paylaşımlı ONNX detector'ı yöneten singleton.
 */
public class CameraManager {

    private static CameraManager instance;

    private SmokeDetector detector;
    private boolean initialized = false;

    private final Map<Integer, CameraWorker>          workers    = new ConcurrentHashMap<>();
    private final Map<Integer, Image>                 frames     = new ConcurrentHashMap<>();
    private final Map<Integer, List<DetectionResult>> detections = new ConcurrentHashMap<>();

    // Per-camera UI listeners
    private final Map<Integer, Consumer<Image>>                 frameListeners     = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<List<DetectionResult>>> detectionListeners = new ConcurrentHashMap<>();

    // Global alarm listener (cameraId, detections)
    private BiConsumer<Integer, List<DetectionResult>> onAlarm;
    private Runnable onInitialized;

    private CameraManager() {}

    public static synchronized CameraManager getInstance() {
        if (instance == null) instance = new CameraManager();
        return instance;
    }

    // ── Başlatma ─────────────────────────────────────────────────────────────

    public void initAsync(String modelPath, String[] classNames, List<Camera> activeCameras) {
        Thread t = new Thread(() -> {
            try {
                if (!initialized) {
                    detector    = new SmokeDetector(modelPath, classNames);
                    initialized = true;
                }
                activeCameras.forEach(this::startCamera);
                if (onInitialized != null)
                    javafx.application.Platform.runLater(onInitialized);
            } catch (Exception e) {
                System.err.println("CameraManager init hata: " + e.getMessage());
            }
        }, "cam-manager-init");
        t.setDaemon(true);
        t.start();
    }

    // ── Kamera kontrolü ──────────────────────────────────────────────────────

    public void startCamera(Camera camera) {
        if (workers.containsKey(camera.getId()) || !initialized) return;

        CameraWorker w = new CameraWorker(detector);
        w.setConfThreshold(AppSettings.getMinimumConfidence() / 100.0f);

        w.setOnFrame(img -> {
            frames.put(camera.getId(), img);
            Consumer<Image> cb = frameListeners.get(camera.getId());
            if (cb != null) cb.accept(img);
        });

        w.setOnDetection(dets -> {
            detections.put(camera.getId(), dets);
            Consumer<List<DetectionResult>> cb = detectionListeners.get(camera.getId());
            if (cb != null) cb.accept(dets);
            if (!dets.isEmpty() && onAlarm != null)
                onAlarm.accept(camera.getId(), dets);
        });

        w.start(camera.getSource());
        workers.put(camera.getId(), w);
    }

    public void stopCamera(int cameraId) {
        CameraWorker w = workers.remove(cameraId);
        if (w != null) w.stop();
        frames.remove(cameraId);
        detections.remove(cameraId);
    }

    public void stopAll() {
        workers.values().forEach(CameraWorker::stop);
        workers.clear();
        if (detector != null) { detector.close(); detector = null; }
        initialized = false;
    }

    // ── Listener yönetimi ────────────────────────────────────────────────────

    public void setOnFrame(int camId, Consumer<Image> cb)                    { frameListeners.put(camId, cb); }
    public void setOnDetection(int camId, Consumer<List<DetectionResult>> cb){ detectionListeners.put(camId, cb); }
    public void removeListeners(int camId) { frameListeners.remove(camId); detectionListeners.remove(camId); }
    public void setOnAlarm(BiConsumer<Integer, List<DetectionResult>> cb)    { this.onAlarm = cb; }
    public void setOnInitialized(Runnable cb)                                { this.onInitialized = cb; }

    // ── Durum / Getter ───────────────────────────────────────────────────────

    public boolean isInitialized()                           { return initialized; }
    public boolean isCameraRunning(int camId)                { return workers.containsKey(camId); }
    public Image getLatestFrame(int camId)                   { return frames.get(camId); }
    public List<DetectionResult> getLatestDetections(int id) { return detections.getOrDefault(id, Collections.emptyList()); }
    public Set<Integer> getRunningCameraIds()                { return workers.keySet(); }
}
