package com.gozcu.ui;

import com.gozcu.inference.CameraManager;
import com.gozcu.inference.DetectionResult;
import com.gozcu.model.Alarm;
import com.gozcu.model.Camera;
import com.gozcu.repository.AlarmRepository;
import com.gozcu.repository.CameraRepository;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;
import com.gozcu.util.AlertSoundPlayer;
import com.gozcu.util.SceneManager;

public class DashboardView {

    private final CameraRepository camRepo   = new CameraRepository();
    private final AlarmRepository  almRepo   = new AlarmRepository();
    private final CameraManager    camMgr    = CameraManager.getInstance();

    // Kamera hücreleri
    private final Map<Integer, ImageView> cellViews   = new HashMap<>();
    private final Map<Integer, Label>     cellBadges  = new HashMap<>();
    private final Map<Integer, VBox>      cellBoxes   = new HashMap<>();

    // Alarm paneli
    private Label activeCamLabel;
    private Label todayAlarmLabel;
    private Label criticalAlarmLabel;
    private VBox  alarmListBox;

    private VBox    root;
    private StackPane focusOverlay;
    private StackPane detailOverlay;
    private Timeline statsTimer;

    // Focused camera (büyük görünüm)
    private int focusedCamId = -1;
    
    // Son algılama zamanı (hücre kırmızılığını yönetmek için)
    private final Map<Integer, Long> lastDetectionMs = new HashMap<>();

    public VBox getView() {
        root = new VBox(0);
        root.setStyle("-fx-background-color: #0f172a;");

        // ── Üst başlık ────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("⬡  GÖZCÜ — Kontrol Merkezi");
        title.setStyle("-fx-text-fill: #f97316; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitle = new Label("Gerçek Zamanlı Yangın / Duman İzleme");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label loadingLabel = new Label(camMgr.isInitialized() ? "" : "⚙ Model yükleniyor...");
        loadingLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");

        camMgr.setOnInitialized(() -> {
            loadingLabel.setText("✔ Sistem hazır");
            loadingLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px;");
            refreshCameraGrid();
        });

        header.getChildren().addAll(title, new Label("  "), subtitle, spacer, loadingLabel);

        // ── Ana içerik ────────────────────────────────────────────────────────
        HBox mainContent = new HBox(0);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Sol: kamera grid
        ScrollPane gridScroll = new ScrollPane();
        gridScroll.setFitToWidth(true);
        gridScroll.setStyle("-fx-background-color: #0f172a; -fx-background: #0f172a;");
        HBox.setHgrow(gridScroll, Priority.ALWAYS);

        GridPane camGrid = buildCameraGrid();
        gridScroll.setContent(camGrid);

        // Sağ: alarm paneli
        VBox alarmPanel = buildAlarmPanel();
        alarmPanel.setPrefWidth(260);
        alarmPanel.setMinWidth(240);

        mainContent.getChildren().addAll(gridScroll, alarmPanel);

        // Focus overlay (kamera büyüme)
        focusOverlay = buildFocusOverlay();
        focusOverlay.setVisible(false);

        // Detail overlay (alarm onaylama/müdahale)
        detailOverlay = new StackPane();
        detailOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
        detailOverlay.setVisible(false);
        detailOverlay.setOnMouseClicked(e -> { if (e.getTarget() == detailOverlay) closeDetailOverlay(); });

        StackPane rootStack = new StackPane(root, focusOverlay, detailOverlay);
        VBox wrapper = new VBox(rootStack);
        VBox.setVgrow(rootStack, Priority.ALWAYS);

        root.getChildren().addAll(header, mainContent);

        // Global alarm listener → hücre kırmızıya döner + loga eklenir
        camMgr.setOnAlarm((camId, dets) -> Platform.runLater(() -> {
            lastDetectionMs.put(camId, System.currentTimeMillis());
            flashCell(camId);
            saveAutoAlarm(camId, dets);
            // Otomatik odaklanmayı kaldırdık, UI'ı bloke etmesin.
        }));

        // Stats timer
        statsTimer = new Timeline(new KeyFrame(Duration.seconds(4), e -> refreshStats()));
        statsTimer.setCycleCount(Animation.INDEFINITE);

        root.sceneProperty().addListener((obs, old, ns) -> {
            if (ns == null) { statsTimer.stop(); camMgr.setOnAlarm(null); clearCellListeners(); }
            else            { statsTimer.play(); refreshStats(); registerCellListeners(); }
        });

        return wrapper;
    }

    // ── Kamera grid ───────────────────────────────────────────────────────────

    private GridPane buildCameraGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(2); grid.setVgap(2);
        grid.setPadding(new Insets(2));
        grid.setStyle("-fx-background-color: #0f172a;");

        List<Camera> cameras = camRepo.findAllCameras();

        if (cameras.isEmpty()) {
            Label empty = new Label("Henüz kamera yok.\nKameralar ekranından kamera ekleyin.");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 16px; -fx-text-alignment: center;");
            empty.setAlignment(Pos.CENTER);
            grid.add(empty, 0, 0);
            return grid;
        }

        int cols = cameras.size() == 1 ? 1 : 2;

        for (int i = 0; i < cameras.size(); i++) {
            Camera cam = cameras.get(i);
            VBox cell  = createCameraCell(cam);
            cellBoxes.put(cam.getId(), cell);
            grid.add(cell, i % cols, i / cols);
        }
        return grid;
    }

    private VBox createCameraCell(Camera camera) {
        // Başlık
        Label nameLabel = new Label("📷  " + camera.getName());
        nameLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label locLabel = new Label(camera.getLocation());
        locLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        HBox cellHeader = new HBox(8, nameLabel);
        cellHeader.setAlignment(Pos.CENTER_LEFT);
        cellHeader.setPadding(new Insets(8, 10, 4, 10));

        // Video alanı
        ImageView iv = new ImageView();
        iv.setFitWidth(440); iv.setFitHeight(300);
        iv.setPreserveRatio(true);
        cellViews.put(camera.getId(), iv);

        StackPane videoPane = new StackPane(iv);
        videoPane.setMinSize(440, 300);
        videoPane.setStyle("-fx-background-color: #111827;");

        Label placeholder = new Label("Kamera bekleniyor...");
        placeholder.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");
        videoPane.getChildren().add(placeholder);

        // Status badge
        Label badge = new Label("● BAĞLANIYOR");
        badge.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10;");
        cellBadges.put(camera.getId(), badge);

        HBox cellFooter = new HBox(8, badge, new Label(camera.getSource().equals("0") ? "Dahili" : "Harici") {{
            setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
        }});
        cellFooter.setAlignment(Pos.CENTER_LEFT);
        cellFooter.setPadding(new Insets(4, 10, 8, 10));

        VBox cell = new VBox(0, cellHeader, videoPane, cellFooter);
        cell.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1;");
        cell.setCursor(javafx.scene.Cursor.HAND);
        cell.setOnMouseClicked(e -> openFocus(camera.getId()));

        return cell;
    }

    private void registerCellListeners() {
        List<Camera> cameras = camRepo.findAllCameras();
        for (Camera cam : cameras) {
            int id = cam.getId();
            camMgr.setOnFrame(id, img -> {
                ImageView iv = cellViews.get(id);
                Label badge  = cellBadges.get(id);
                if (iv == null) return;
                Platform.runLater(() -> {
                    iv.setImage(img);
                    if (badge != null) {
                        // Eğer hücre alarm durumundaysa "CANLI" yazıp kırmızı çerçeveyi/yazıyı bozma
                        long lastDet = lastDetectionMs.getOrDefault(id, 0L);
                        if (lastDet == 0 || System.currentTimeMillis() - lastDet > 5000) {
                            if (!"● CANLI".equals(badge.getText())) {
                                badge.setText("● CANLI");
                                badge.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10;");
                            }
                        }
                    }
                    // placeholder'ı kaldır
                    StackPane pane = (StackPane) iv.getParent();
                    if (pane != null) pane.getChildren().removeIf(n -> n instanceof Label);
                });
            });
        }
    }

    private void clearCellListeners() {
        cellViews.keySet().forEach(camMgr::removeListeners);
    }

    private void refreshCameraGrid() {
        // Modeli yükledikten sonra listener'ları aktive et
        registerCellListeners();
    }

    // ── Focus overlay ─────────────────────────────────────────────────────────

    private StackPane buildFocusOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");

        VBox focusBox = new VBox(12);
        focusBox.setMaxWidth(820);
        focusBox.setMaxHeight(620);
        focusBox.setPadding(new Insets(20));
        focusBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16;");
        focusBox.setAlignment(Pos.TOP_LEFT);

        // Başlık
        Label focusTitle = new Label("Kamera Odak");
        focusTitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 18px; -fx-font-weight: bold;");
        focusTitle.setId("focus-title");

        Button closeBtn = new Button("✕ Kapat");
        closeBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> closeFocus());

        HBox focusHeader = new HBox(focusTitle);
        focusHeader.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        focusHeader.getChildren().addAll(sp, closeBtn);

        // Büyük video
        ImageView bigView = new ImageView();
        bigView.setFitWidth(780); bigView.setFitHeight(500);
        bigView.setPreserveRatio(true);
        bigView.setId("focus-imageview");

        StackPane bigPane = new StackPane(bigView);
        bigPane.setStyle("-fx-background-color: #000;");
        bigPane.setMinSize(780, 500);

        // Alarm badge
        Label alarmLabel = new Label();
        alarmLabel.setId("focus-alarm");
        alarmLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-font-weight: bold;");

        focusBox.getChildren().addAll(focusHeader, bigPane, alarmLabel);
        overlay.getChildren().add(focusBox);
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) closeFocus(); });

        return overlay;
    }

    private void openFocus(int cameraId) {
        focusedCamId = cameraId;
        focusOverlay.setVisible(true);

        // Başlık güncelle
        Camera cam = camRepo.findAllCameras().stream()
                .filter(c -> c.getId() == cameraId).findFirst().orElse(null);
        Label titleLbl = (Label) focusOverlay.lookup("#focus-title");
        if (titleLbl != null && cam != null)
            titleLbl.setText(cam.getName() + " — " + cam.getLocation());

        // Büyük ImageView'a frame bağla
        ImageView bigView = (ImageView) focusOverlay.lookup("#focus-imageview");
        if (bigView != null) {
            // İlk frame varsa hemen göster
            if (camMgr.getLatestFrame(cameraId) != null)
                bigView.setImage(camMgr.getLatestFrame(cameraId));

            camMgr.setOnFrame(cameraId, img -> {
                if (focusedCamId == cameraId) Platform.runLater(() -> bigView.setImage(img));
            });

            camMgr.setOnDetection(cameraId, dets -> Platform.runLater(() -> {
                Label al = (Label) focusOverlay.lookup("#focus-alarm");
                if (al == null) return;
                if (dets.isEmpty()) { al.setText(""); return; }
                DetectionResult best = dets.stream().max(
                        Comparator.comparingDouble(DetectionResult::getConfidence)).orElse(null);
                if (best != null)
                    al.setText("⚠  " + best.getClassName().toUpperCase() + " TESPİT EDİLDİ — %" + best.getConfidencePct());
            }));
        }
    }

    private void closeFocus() {
        if (focusedCamId != -1) {
            // listener'ları yeniden grid moduna bağla
            registerCellListeners();
            focusedCamId = -1;
        }
        focusOverlay.setVisible(false);
    }

    // ── Alarm paneli ──────────────────────────────────────────────────────────

    private VBox buildAlarmPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 0 1;");

        Label panelTitle = new Label("DURUM PANELİ");
        panelTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 14 16;");

        // İstatistik kartları
        activeCamLabel   = statLabel("0");
        activeCamLabel.setId("activeCamLabel");
        todayAlarmLabel  = statLabel("0");
        todayAlarmLabel.setId("todayAlarmLabel");
        criticalAlarmLabel = statLabel("0");
        criticalAlarmLabel.setId("criticalAlarmLabel");

        VBox stats = new VBox(2,
                statCard("Aktif Kamera",  activeCamLabel),
                statCard("Bugünkü Alarm", todayAlarmLabel),
                statCard("Kritik Alarm",  criticalAlarmLabel)
        );
        stats.setPadding(new Insets(0, 12, 12, 12));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        Label alarmsTitle = new Label("SON ALARMLAR");
        alarmsTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 12 16 6 16;");

        alarmListBox = new VBox(4);
        alarmListBox.setPadding(new Insets(0, 12, 12, 12));

        ScrollPane alarmScroll = new ScrollPane(alarmListBox);
        alarmScroll.setFitToWidth(true);
        alarmScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(alarmScroll, Priority.ALWAYS);

        panel.getChildren().addAll(panelTitle, stats, sep, alarmsTitle, alarmScroll);
        return panel;
    }

    private HBox statCard(String label, Label valueLabel) {
        VBox card = new VBox(2, new Label(label) {{ setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;"); }}, valueLabel);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color:#0f172a;-fx-background-radius:8;");
        HBox wrap = new HBox(card);
        HBox.setHgrow(card, Priority.ALWAYS);
        return wrap;
    }

    private Label statLabel(String val) {
        Label l = new Label(val);
        l.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 26px; -fx-font-weight: bold;");
        return l;
    }

    private void refreshStats() {
        activeCamLabel.setText(String.valueOf(camRepo.countActiveCameras()));
        todayAlarmLabel.setText(String.valueOf(almRepo.countTodayAlarms()));
        criticalAlarmLabel.setText(String.valueOf(almRepo.countCriticalAlarms()));

        alarmListBox.getChildren().clear();
        almRepo.findRecentAlarms(8).forEach(a -> {
            String time  = a.getDetectionTime().length() >= 16 ? a.getDetectionTime().substring(11, 16) : a.getDetectionTime();
            HBox row = new HBox(8);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-color:#0f172a;-fx-background-radius:6;");

            Label t = new Label(time); t.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");
            Label info = new Label(a.getCameraName() + "\n" + a.getAlarmType());
            info.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12px;");
            VBox.setVgrow(info, Priority.ALWAYS);
            
            VBox badgeBox = new VBox(4);
            badgeBox.setAlignment(Pos.CENTER_RIGHT);
            Label lvl = new Label(a.getLevel()); lvl.setStyle(levelStyle(a.getLevel()));
            
            Button detailBtn = new Button("Müdahale Et →");
            detailBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0;");
            detailBtn.setOnAction(e -> {
                AlertSoundPlayer.getInstance().stop(); // Tıklandığı an sesi sustur
                showDetailOverlay(a);
            });
            
            badgeBox.getChildren().addAll(lvl, detailBtn);

            row.getChildren().addAll(t, info, badgeBox);
            HBox.setHgrow(info, Priority.ALWAYS);
            alarmListBox.getChildren().add(row);
        });
        
        // 5 saniyeden uzun süredir algılama olmayan kameraları normale döndür
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, VBox> entry : cellBoxes.entrySet()) {
            int camId = entry.getKey();
            if (lastDetectionMs.getOrDefault(camId, 0L) > 0 && now - lastDetectionMs.get(camId) > 5000) {
                lastDetectionMs.put(camId, 0L);
                entry.getValue().setStyle("-fx-background-color:#1e293b;-fx-border-color:#334155;-fx-border-width:1;");
                Label badge = cellBadges.get(camId);
                if (badge != null && badge.getText().equals("⚠ ALARM")) {
                    badge.setText("● CANLI"); 
                    badge.setStyle("-fx-text-fill:#22c55e;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:4 10;");
                }
            }
        }
    }

    private void showDetailOverlay(Alarm a) {
        VBox detailView = new AlarmDetailView(a, this::closeDetailOverlay).getView();
        detailView.setMaxWidth(600);
        detailView.setMaxHeight(600);
        // Arka planı şeffaf yapıp kartın kendi tasarımını kullanacağız
        detailView.setStyle("-fx-background-color: transparent;"); 
        
        detailOverlay.getChildren().clear();
        detailOverlay.getChildren().add(detailView);
        detailOverlay.setVisible(true);
    }

    private void closeDetailOverlay() {
        detailOverlay.setVisible(false);
        refreshStats(); // Listeyi güncelle
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private void flashCell(int cameraId) {
        VBox cell = cellBoxes.get(cameraId);
        Label badge = cellBadges.get(cameraId);
        if (cell == null) return;

        badge.setText("⚠ ALARM"); badge.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:4 10;");
        cell.setStyle("-fx-background-color:#1e293b;-fx-border-color:#ef4444;-fx-border-width:2;");
        
        // Timeline kaldırıldı; artık kırmızılık refreshStats() içinde 
        // lastDetectionMs kontrol edilerek normale döndürülüyor.
    }

    private long lastAlarmSaveMs = 0;
    private void saveAutoAlarm(int cameraId, List<DetectionResult> dets) {
        long now = System.currentTimeMillis();
        if (now - lastAlarmSaveMs < 10_000) return;
        lastAlarmSaveMs = now;

        camRepo.findAllCameras().stream().filter(c -> c.getId() == cameraId).findFirst().ifPresent(cam -> {
            DetectionResult best = dets.stream().max(Comparator.comparingDouble(DetectionResult::getConfidence)).orElse(null);
            if (best == null) return;
            int pct = best.getConfidencePct();
            String level = pct >= 80 ? "Kritik" : pct >= 50 ? "Orta" : "Düşük";
            String time  = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            almRepo.saveAlarm(new Alarm(cam.getName(), cam.getLocation(), best.getClassName(), level, best.getConfidence(), "Yeni", time, "Otomatik tespit"));
            
            // Eğer uyarı sesleri açıksa ve seviye Kritik/Orta ise alarm çal (NFPA 72 §18.4.4)
            if ("Kritik".equals(level) || "Orta".equals(level)) {
                AlertSoundPlayer.getInstance().playFireAlarm();
            }
        });
    }

    private String levelStyle(String level) {
        return switch (level) {
            case "Kritik" -> "-fx-background-color:#450a0a;-fx-text-fill:#ef4444;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 6;-fx-background-radius:4;";
            case "Orta"   -> "-fx-background-color:#431407;-fx-text-fill:#f97316;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 6;-fx-background-radius:4;";
            default        -> "-fx-background-color:#172554;-fx-text-fill:#3b82f6;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 6;-fx-background-radius:4;";
        };
    }
}