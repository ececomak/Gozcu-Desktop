package com.gozcu.ui;

import com.gozcu.inference.DetectionResult;
import com.gozcu.inference.SmokeDetector;
import com.gozcu.util.AppSettings;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Video dosyası yükleyerek ONNX modeli ile yangın/duman tespiti yapan test ekranı.
 */
public class VideoTestView {

    private static final int SKIP = 5; // Her N karede 1 inference

    private ImageView  videoView;
    private Label      statusLabel;
    private Label      confLabel;
    private ProgressBar progressBar;
    private ProgressBar confBar;
    private Button     playBtn;
    private Button     stopBtn;
    private VBox       detectionList;
    private Label      totalFrameVal, processedVal, detectedVal, rateVal;
    private Slider     thresholdSlider;

    private File selectedFile = null;

    private final AtomicBoolean running   = new AtomicBoolean(false);
    private ExecutorService     executor;

    // İstatistikler
    private int    totalFrames     = 0;
    private int    detectedFrames  = 0;
    private int    processedFrames = 0;
    private double maxConfidence   = 0;

    public VBox getView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f172a;");

        // ── Başlık ─────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(18, 28, 18, 28));
        header.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🎬  Video ile Model Testi");
        title.setStyle("-fx-text-fill: #f97316; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label desc = new Label("  —  Video yükleyin, model her kareyi analiz etsin");
        desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        header.getChildren().addAll(title, desc);

        // ── Kontrol çubuğu (her zaman görünür, ayrı blok) ──────────────────
        HBox toolbar = buildToolbar();
        toolbar.setPadding(new Insets(12, 20, 12, 20));
        toolbar.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        // ── İlerleme çubuğu ────────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);
        progressBar.setStyle("-fx-accent: #f97316;");

        // ── Ana içerik (video | panel) ──────────────────────────────────────
        HBox content = new HBox(16);
        content.setPadding(new Insets(16, 20, 16, 20));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Video alanı
        StackPane videoPane = new StackPane();
        videoPane.setStyle("-fx-background-color: #020617; -fx-background-radius: 10;");
        videoPane.setMinSize(580, 400);
        videoPane.setPrefSize(700, 460);
        HBox.setHgrow(videoPane, Priority.ALWAYS);

        Label placeholder = new Label("📂  Video seçin ve Analizi Başlat'a tıklayın");
        placeholder.setStyle("-fx-text-fill: #334155; -fx-font-size: 16px;");

        videoView = new ImageView();
        videoView.setFitWidth(700);
        videoView.setFitHeight(460);
        videoView.setPreserveRatio(true);

        videoPane.getChildren().addAll(placeholder, videoView);

        // Analiz paneli
        VBox analysisPanel = buildAnalysisPanel();
        analysisPanel.setPrefWidth(290);
        analysisPanel.setMinWidth(260);

        content.getChildren().addAll(videoPane, analysisPanel);

        // ── Durum çubuğu ──────────────────────────────────────────────────
        statusLabel = new Label("Hazır — Video seçin");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        statusLabel.setPadding(new Insets(6, 20, 8, 20));

        root.getChildren().addAll(header, toolbar, progressBar, content, statusLabel);
        return root;
    }

    // ── Araç çubuğu ───────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        Button openBtn = new Button("📂  Video Seç");
        openBtn.setId("openVideoBtn");
        openBtn.setStyle(btnStyle("#334155"));
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Video Dosyası Seç");
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Dosyaları", "*.mp4","*.avi","*.mov","*.mkv","*.wmv"),
                new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
            );
            File file = fc.showOpenDialog(openBtn.getScene().getWindow());
            if (file != null) {
                selectedFile = file;
                playBtn.setDisable(false);
                setStatus("✔  Seçildi: " + file.getName(), "#22c55e");
            }
        });

        playBtn = new Button("▶  Analizi Başlat");
        playBtn.setId("playVideoBtn");
        playBtn.setStyle(btnStyle("#16a34a"));
        playBtn.setDisable(true);
        playBtn.setOnAction(e -> startAnalysis());

        stopBtn = new Button("⏹  Durdur");
        stopBtn.setId("stopVideoBtn");
        stopBtn.setStyle(btnStyle("#dc2626"));
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> stopAnalysis());

        Label threshLbl = new Label("Güven Eşiği:");
        threshLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        thresholdSlider = new Slider(10, 95, AppSettings.getMinimumConfidence());
        thresholdSlider.setPrefWidth(130);

        Label threshVal = new Label("%" + AppSettings.getMinimumConfidence());
        threshVal.setStyle("-fx-text-fill: #f97316; -fx-font-size: 12px; -fx-font-weight: bold;");
        thresholdSlider.valueProperty().addListener((obs, o, n) ->
            threshVal.setText("%" + n.intValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, openBtn, playBtn, stopBtn, spacer, threshLbl, thresholdSlider, threshVal);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ── Analiz paneli ──────────────────────────────────────────────────────────

    private VBox buildAnalysisPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color:#1e293b;-fx-background-radius:10;-fx-border-radius:10;-fx-border-color:#334155;");

        Label panelTitle = new Label("Analiz Sonuçları");
        panelTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f1f5f9;");

        Label confTitle = new Label("En Yüksek Güven Skoru");
        confTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");

        confLabel = new Label("%0");
        confLabel.setStyle("-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:#22c55e;");

        confBar = new ProgressBar(0);
        confBar.setPrefWidth(Double.MAX_VALUE);
        confBar.setStyle("-fx-accent:#22c55e;");

        // Stat satırları
        totalFrameVal   = statValueLabel();
        processedVal    = statValueLabel();
        detectedVal     = statValueLabel();
        rateVal         = statValueLabel();

        GridPane stats = new GridPane();
        stats.setHgap(8); stats.setVgap(6);
        addStatRow(stats, 0, "Toplam Kare:",   totalFrameVal);
        addStatRow(stats, 1, "İşlenen Kare:",  processedVal);
        addStatRow(stats, 2, "Tespit Edilen:", detectedVal);
        addStatRow(stats, 3, "Tespit Oranı:",  rateVal);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#334155;");

        Label recentTitle = new Label("SON TESPİTLER");
        recentTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;-fx-font-weight:bold;");

        detectionList = new VBox(5);
        ScrollPane scroll = new ScrollPane(detectionList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(
            panelTitle, confTitle, confLabel, confBar,
            new Separator(), stats, sep, recentTitle, scroll
        );
        return panel;
    }

    private Label statValueLabel() {
        Label l = new Label("—");
        l.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private void addStatRow(GridPane g, int row, String label, Label val) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        g.add(lbl, 0, row);
        g.add(val, 1, row);
    }

    // ── Analiz mantığı ────────────────────────────────────────────────────────

    private void startAnalysis() {
        if (selectedFile == null || running.get()) return;

        // Sıfırla
        totalFrames = 0; detectedFrames = 0; processedFrames = 0; maxConfidence = 0;
        updateStats();
        detectionList.getChildren().clear();
        progressBar.setProgress(0);
        videoView.setImage(null);

        running.set(true);
        playBtn.setDisable(true);
        stopBtn.setDisable(false);
        setStatus("⚙  Model yükleniyor...", "#f59e0b");

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "video-analysis");
            t.setDaemon(true);
            return t;
        });
        final float conf = (float) thresholdSlider.getValue() / 100f;
        executor.submit(() -> analyzeVideo(selectedFile, conf));
    }

    private void analyzeVideo(File videoFile, float confThreshold) {
        SmokeDetector detector = null;
        try {
            String modelPath = com.gozcu.MainApp.resolveModelPath();
            detector = new SmokeDetector(modelPath, AppSettings.getClassNames());

            Platform.runLater(() -> setStatus("▶  Analiz ediliyor...", "#22c55e"));

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getAbsolutePath());
            grabber.start();

            totalFrames = grabber.getLengthInVideoFrames();
            final int total = Math.max(totalFrames, 1);

            Java2DFrameConverter converter = new Java2DFrameConverter();
            int frameIdx = 0;
            Frame frame;

            while (running.get() && (frame = grabber.grabImage()) != null) {
                frameIdx++;
                if (frameIdx % SKIP != 0) continue;

                BufferedImage img = converter.getBufferedImage(frame);
                if (img == null) continue;

                processedFrames++;
                List<DetectionResult> dets = detector.detect(img, confThreshold);
                BufferedImage annotated = detector.annotate(img, dets);
                Image fxImg = toFXImage(annotated);

                if (!dets.isEmpty()) {
                    detectedFrames++;
                    dets.stream()
                        .mapToDouble(d -> d.getConfidence())
                        .max().ifPresent(c -> { if (c > maxConfidence) maxConfidence = c; });
                }

                final int fi = frameIdx;
                final List<DetectionResult> snap = dets;
                final double progress = (double) frameIdx / total;

                Platform.runLater(() -> {
                    videoView.setImage(fxImg);
                    progressBar.setProgress(progress);
                    updateStats();
                    if (!snap.isEmpty()) {
                        snap.stream()
                            .max((a, b) -> Float.compare(a.getConfidence(), b.getConfidence()))
                            .ifPresent(best -> addDetectionEntry(fi, best));
                    }
                });
            }

            grabber.stop();
            grabber.release();

            Platform.runLater(() -> {
                running.set(false);
                playBtn.setDisable(false);
                stopBtn.setDisable(true);
                progressBar.setProgress(1.0);
                setStatus("✔  Tamamlandı — " + processedFrames + " kare işlendi, " + detectedFrames + " tespit", "#22c55e");
            });

        } catch (Exception e) {
            final String msg = e.getMessage();
            Platform.runLater(() -> {
                setStatus("✘  Hata: " + msg, "#ef4444");
                running.set(false);
                playBtn.setDisable(false);
                stopBtn.setDisable(true);
            });
        } finally {
            if (detector != null) detector.close();
        }
    }

    private void stopAnalysis() {
        running.set(false);
        if (executor != null) { executor.shutdownNow(); executor = null; }
        playBtn.setDisable(false);
        stopBtn.setDisable(true);
        setStatus("⏹  Durduruldu — " + processedFrames + " kare işlendi", "#64748b");
    }

    // ── UI yardımcıları ───────────────────────────────────────────────────────

    private void updateStats() {
        int pct = (int) Math.round(maxConfidence * 100);
        confLabel.setText("%" + pct);
        confBar.setProgress(maxConfidence);
        double rate = processedFrames > 0 ? (double) detectedFrames / processedFrames * 100 : 0;

        String color = pct >= 70 ? "#ef4444" : pct >= 40 ? "#f97316" : "#22c55e";
        confLabel.setStyle("-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        confBar.setStyle("-fx-accent:" + color + ";");

        totalFrameVal.setText(String.valueOf(totalFrames));
        processedVal.setText(String.valueOf(processedFrames));
        detectedVal.setText(String.valueOf(detectedFrames));
        rateVal.setText(String.format("%.1f%%", rate));
    }

    private void addDetectionEntry(int frameNo, DetectionResult det) {
        if (detectionList.getChildren().size() > 40)
            detectionList.getChildren().remove(0);

        String color = det.getClassName().equalsIgnoreCase("fire") ? "#ef4444" : "#f97316";
        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color:#0f172a;-fx-background-radius:5;");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lFrame = new Label("K" + frameNo);
        lFrame.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
        lFrame.setMinWidth(50);

        Label lType = new Label(det.getClassName().toUpperCase());
        lType.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label lConf = new Label("%" + det.getConfidencePct());
        lConf.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");

        row.getChildren().addAll(lFrame, lType, sp, lConf);
        detectionList.getChildren().add(row);
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;");
    }

    private String btnStyle(String bg) {
        return "-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-font-weight:bold;" +
               "-fx-padding:8 16;-fx-background-radius:7;-fx-cursor:hand;";
    }

    private Image toFXImage(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        WritableImage wi = new WritableImage(w, h);
        wi.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        return wi;
    }
}
