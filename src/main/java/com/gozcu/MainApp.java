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
import com.gozcu.ui.LoginView;

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

        // İlk olarak Login ekranını göster
        LoginView loginView = new LoginView(stage);
        loginView.show();
        
        stage.setTitle("Gözcü Desktop");
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
            
            // Sistemde sanal olan kameraların indexlerini (source) bul
            Set<String> virtualSources = new java.util.HashSet<>();
            for (int i = 0; i < systemCams.size(); i++) {
                String camName = systemCams.get(i).getName().toLowerCase();
                if (camName.contains("virtual") || camName.contains("obs") ||
                    camName.contains("manycam") || camName.contains("droidcam")) {
                    virtualSources.add(String.valueOf(i));
                    System.out.println("Sanal kamera atlandı: " + systemCams.get(i).getName());
                }
            }
            
            // Veritabanındaki kameraları kontrol et, source'u sanal olanları sil
            for (Camera c : repo.findAllCameras()) {
                if (virtualSources.contains(c.getSource())) {
                    repo.deleteCamera(c.getId());
                    System.out.println("Sanal kamera (source=" + c.getSource() + ") veritabanından silindi.");
                }
            }

            Set<String> dbSources   = new java.util.HashSet<>();
            repo.findAllCameras().forEach(c -> dbSources.add(c.getSource()));

            int addedCount = 0;
            for (int i = 0; i < systemCams.size(); i++) {
                String src = String.valueOf(i);
                if (virtualSources.contains(src)) continue; // Sanalsa geç
                
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