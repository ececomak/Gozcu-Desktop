package com.gozcu.ui;

import com.gozcu.inference.CameraManager;
import com.gozcu.inference.DetectionResult;
import com.gozcu.model.Camera;
import com.gozcu.repository.CameraRepository;
import com.gozcu.util.AppSettings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

/**
 * CameraManager'dan mevcut stream'i okur — kopyası yok.
 * Operatör bir kamerayı seçip odaklanabilir.
 */
public class LiveMonitorView {

    private final CameraRepository camRepo = new CameraRepository();
    private final CameraManager    camMgr  = CameraManager.getInstance();

    private ImageView   cameraView;
    private Label       statusBadge;
    private Label       confidenceLabel;
    private ProgressBar confidenceBar;
    private Label       detectionTypeLabel;
    private Label       locationLabel;
    private Label       fpsLabel;

    private int focusedCamId = -1;

    public VBox getView() { return getView(null); }

    public VBox getView(Camera preselected) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0f172a;");

        // ── Başlık ───────────────────────────────────────────────────────────
        Label title = new Label("Canlı İzleme — Odak Modu");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label desc = new Label("Seçili kameranın büyük görünümü ve tespit detayları.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // ── Kamera seçimi ─────────────────────────────────────────────────────
        List<Camera> cameras = camRepo.findAllCameras();
        ComboBox<Camera> combo = new ComboBox<>(FXCollections.observableArrayList(cameras));
        combo.setPrefWidth(260);
        combo.setPromptText("Kamera seç...");
        if (preselected != null) combo.setValue(preselected);
        else if (!cameras.isEmpty()) combo.setValue(cameras.get(0));

        statusBadge = new Label("● —");
        statusBadge.setStyle(styleNormal());
        fpsLabel = new Label("FPS: —");
        fpsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Button focusBtn = new Button("⬡  Odaklan");
        focusBtn.setStyle("-fx-background-color:#f97316;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:10;-fx-cursor:hand;");
        focusBtn.setOnAction(e -> {
            Camera cam = combo.getValue();
            if (cam != null) focusCamera(cam);
        });

        HBox controls = new HBox(12, combo, statusBadge, fpsLabel, focusBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        // ── Video + Panel ─────────────────────────────────────────────────────
        HBox content = new HBox(16);

        StackPane camPane = new StackPane();
        camPane.setMinSize(660, 496);
        camPane.setPrefSize(700, 525);
        camPane.setStyle("-fx-background-color: #020617; -fx-background-radius: 14;");

        cameraView = new ImageView();
        cameraView.setFitWidth(700); cameraView.setFitHeight(525);
        cameraView.setPreserveRatio(true);

        Label placeholder = new Label("Kamera seçin ve Odaklan'a basın");
        placeholder.setStyle("-fx-text-fill: #334155; -fx-font-size: 16px;");

        camPane.getChildren().addAll(placeholder, cameraView);

        VBox panel = buildDetectionPanel();
        panel.setPrefWidth(300);

        content.getChildren().addAll(camPane, panel);
        root.getChildren().addAll(title, desc, controls, content);

        // Sayfa kapanınca listener temizle
        root.sceneProperty().addListener((obs, old, ns) -> {
            if (ns == null && focusedCamId != -1) {
                camMgr.removeListeners(focusedCamId);
                focusedCamId = -1;
            }
        });

        // Preselected varsa hemen odakla
        if (preselected != null) {
            Platform.runLater(() -> focusCamera(preselected));
        }

        return root;
    }

    // ── Odaklanma ─────────────────────────────────────────────────────────────

    private void focusCamera(Camera camera) {
        // Eski listener'ı temizle
        if (focusedCamId != -1) camMgr.removeListeners(focusedCamId);
        focusedCamId = camera.getId();

        // Model henüz yüklenmedi mi?
        if (!camMgr.isInitialized()) {
            statusBadge.setText("⚙ Model yükleniyor...");
            statusBadge.setStyle(styleLoading());
            return;
        }

        // Kamera çalışıyor mu?
        if (!camMgr.isCameraRunning(camera.getId())) {
            statusBadge.setText("● BAĞLANIYOR");
            statusBadge.setStyle(styleNormal());
        }

        // Frame listener
        camMgr.setOnFrame(camera.getId(), img -> Platform.runLater(() -> {
            cameraView.setImage(img);
            statusBadge.setText("● CANLI");
            statusBadge.setStyle(styleLive());
        }));

        // Detection listener
        camMgr.setOnDetection(camera.getId(), dets -> Platform.runLater(() -> {
            if (dets.isEmpty()) {
                confidenceLabel.setText("%0");
                confidenceBar.setProgress(0);
                detectionTypeLabel.setText("—");
                statusBadge.setText("● CANLI");
                statusBadge.setStyle(styleLive());
                return;
            }
            DetectionResult best = dets.stream()
                    .max((a, b) -> Float.compare(a.getConfidence(), b.getConfidence())).orElse(null);
            if (best == null) return;
            int pct = best.getConfidencePct();
            confidenceLabel.setText("%" + pct);
            confidenceBar.setProgress(pct / 100.0);
            detectionTypeLabel.setText(best.getClassName().toUpperCase() + " (" + dets.size() + " nesne)");
            locationLabel.setText(camera.getLocation());
            statusBadge.setText("⚠  YANGIN/DUMAN");
            statusBadge.setStyle(styleDanger());
        }));

        // FPS listener
        camMgr.setOnFrame(camera.getId(), img -> Platform.runLater(() -> cameraView.setImage(img)));

        // Son mevcut frame'i hemen göster
        if (camMgr.getLatestFrame(camera.getId()) != null)
            cameraView.setImage(camMgr.getLatestFrame(camera.getId()));
    }

    // ── Detection paneli ──────────────────────────────────────────────────────

    private VBox buildDetectionPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color:#1e293b;-fx-background-radius:14;-fx-border-radius:14;-fx-border-color:#334155;");

        Label t = new Label("Tespit Bilgisi");
        t.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#f1f5f9;");

        Label confTitle = new Label("Güven Oranı");
        confTitle.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        confidenceLabel = new Label("%0");
        confidenceLabel.setStyle("-fx-font-size:36px;-fx-font-weight:bold;-fx-text-fill:#f1f5f9;");

        confidenceBar = new ProgressBar(0);
        confidenceBar.setPrefWidth(260);
        confidenceBar.setStyle("-fx-accent:#f97316;");

        detectionTypeLabel = new Label("—");
        locationLabel      = new Label("—");

        panel.getChildren().addAll(t, confTitle, confidenceLabel, confidenceBar,
                row("Tespit", detectionTypeLabel),
                row("Konum",  locationLabel),
                new Separator(),
                note("Canlı İzleme ekranı CameraManager'dan mevcut\nstream'i okur. Ayrı bir bağlantı açılmaz.")
        );
        return panel;
    }

    private HBox row(String lbl, Label val) {
        HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl + ":"); l.setMinWidth(70);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;-fx-font-weight:bold;");
        val.setStyle("-fx-font-size:13px;-fx-text-fill:#e2e8f0;");
        r.getChildren().addAll(l, val); return r;
    }

    private Label note(String text) {
        Label n = new Label(text);
        n.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;");
        n.setWrapText(true); return n;
    }

    private String styleNormal()  { return "-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-font-weight:bold;-fx-padding:8 14;-fx-background-radius:20;"; }
    private String styleLive()    { return "-fx-background-color:#052e16;-fx-text-fill:#22c55e;-fx-font-weight:bold;-fx-padding:8 14;-fx-background-radius:20;"; }
    private String styleDanger()  { return "-fx-background-color:#450a0a;-fx-text-fill:#ef4444;-fx-font-weight:bold;-fx-padding:8 14;-fx-background-radius:20;"; }
    private String styleLoading() { return "-fx-background-color:#422006;-fx-text-fill:#f59e0b;-fx-font-weight:bold;-fx-padding:8 14;-fx-background-radius:20;"; }
}