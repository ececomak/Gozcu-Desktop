package com.gozcu;

import com.github.sarxos.webcam.Webcam;
import com.gozcu.inference.CameraManager;
import com.gozcu.model.Camera;
import com.gozcu.repository.CameraRepository;
import com.gozcu.ui.MainLayout;
import com.gozcu.util.AppSettings;
import com.gozcu.util.DatabaseManager;
import com.gozcu.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Set;

public class MainApp extends Application {

    private static final String MODEL_RESOURCE = "/com/gozcu/img_proc_model/best.onnx";

    @Override
    public void start(Stage stage) throws Exception {
        AppSettings.load();
        DatabaseManager.initializeDatabase();

        // Sistem kameralarını DB ile eşitle (yeni kameraları otomatik ekle)
        autoDetectWebcams();

        MainLayout layout = new MainLayout();
        Scene scene = new Scene(layout.getLayout(), 1200, 750);
        ThemeManager.register(scene); // CSS temayı uygula
        stage.setTitle("Gözcü Desktop");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

        // Tüm aktif kameraları ve ONNX modelini arka planda başlat
        CameraRepository repo = new CameraRepository();
        List<Camera> activeCams = repo.findAllCameras().stream()
                .filter(c -> "Aktif".equals(c.getStatus()))
                .toList();

        String modelPath = resolveModelPath();
        CameraManager.getInstance().initAsync(modelPath, AppSettings.getClassNames(), activeCams);
    }

    @Override
    public void stop() {
        CameraManager.getInstance().stopAll();
        DatabaseManager.closeConnection();
    }

    // ── Webcam otomatik keşif ─────────────────────────────────────────────────

    private void autoDetectWebcams() {
        try {
            List<Webcam> systemCams = Webcam.getWebcams();
            CameraRepository repo   = new CameraRepository();
            Set<String> dbSources   = new java.util.HashSet<>();
            repo.findAllCameras().forEach(c -> dbSources.add(c.getSource()));

            int addedCount = 0;
            for (int i = 0; i < systemCams.size(); i++) {
                String camName = systemCams.get(i).getName().toLowerCase();
                // Sanal kameraları atla (OBS, ManyCam, DroidCam vb.)
                if (camName.contains("virtual") || camName.contains("obs") ||
                    camName.contains("manycam") || camName.contains("droidcam")) {
                    System.out.println("Sanal kamera atlandı: " + systemCams.get(i).getName());
                    continue;
                }
                String src = String.valueOf(i);
                if (!dbSources.contains(src)) {
                    String label = addedCount == 0 ? "Dahili Kamera" : "Harici Kamera " + addedCount;
                    repo.saveCamera(new Camera(label, "Otomatik Keşif", src, "Aktif", 60));
                    System.out.println("Yeni kamera eklendi: " + label + " (" + systemCams.get(i).getName() + ")");
                    addedCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("Webcam keşif hatası: " + e.getMessage());
        }
    }

    // ── Model yolu ────────────────────────────────────────────────────────────

    static String resolveModelPath() throws Exception {
        URL url = MainApp.class.getResource(MODEL_RESOURCE);
        if (url != null && url.getProtocol().equals("file"))
            return Paths.get(url.toURI()).toString();
        Path tmp = Files.createTempFile("gozcu_", ".onnx");
        tmp.toFile().deleteOnExit();
        try (InputStream in = MainApp.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (in == null) throw new IllegalStateException("Model bulunamadı: " + MODEL_RESOURCE);
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp.toString();
    }

    public static void main(String[] args) { launch(args); }
}