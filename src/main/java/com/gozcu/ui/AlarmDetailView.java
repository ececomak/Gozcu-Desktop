package com.gozcu.ui;

import com.gozcu.controller.AlarmController;
import com.gozcu.model.Alarm;
import com.gozcu.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AlarmDetailView {

    private final Alarm alarm;
    private final Runnable onCloseCallback;

    public AlarmDetailView(Alarm alarm) {
        this(alarm, null);
    }

    public AlarmDetailView(Alarm alarm, Runnable onCloseCallback) {
        this.alarm = alarm;
        this.onCloseCallback = onCloseCallback;
    }
    
    private void closeView() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            SceneManager.showPage(new AlarmHistoryView().getView());
        }
    }

    public VBox getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;"); // Koyu tema arka plan

        // ── Başlık Bölümü ──────────────────────────────────────────────────
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(alarm.getAlarmType().toLowerCase().contains("fire") ? "🔥" : "💨");
        icon.setStyle("-fx-font-size: 32px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Alarm Detayı — " + alarm.getCameraName());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label timeLabel = new Label(alarm.getDetectionTime());
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(alarm.getLevel().toUpperCase());
        String badgeStyle = switch (alarm.getLevel()) {
            case "Kritik" -> "-fx-background-color: #450a0a; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-font-size: 14px;";
            case "Orta"   -> "-fx-background-color: #431407; -fx-text-fill: #f97316; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-font-size: 14px;";
            default       -> "-fx-background-color: #172554; -fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-font-size: 14px;";
        };
        badge.setStyle(badgeStyle);

        header.getChildren().addAll(icon, titleBox, spacer, badge);

        // ── Kart İçeriği ───────────────────────────────────────────────────
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background-color: #1e293b;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: #334155;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);"
        );

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);

        addRow(grid, 0, "Alarm ID", String.valueOf(alarm.getId()));
        addRow(grid, 1, "Kamera", alarm.getCameraName());
        addRow(grid, 2, "Konum", alarm.getLocation());
        addRow(grid, 3, "Tür", alarm.getAlarmType());
        addRow(grid, 4, "Güven Skoru", "%" + (int) (alarm.getConfidence() * 100));
        addRow(grid, 5, "Mevcut Durum", alarm.getStatus());

        // Operatör Notu Alanı
        VBox noteBox = new VBox(8);
        Label noteLabel = new Label("Operatör Notu:");
        noteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        TextArea noteArea = new TextArea(alarm.getNote() != null ? alarm.getNote() : "");
        noteArea.setPromptText("Alarm ile ilgili notunuzu girin...");
        noteArea.setPrefRowCount(3);
        noteArea.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: #e2e8f0; -fx-border-color: #334155; -fx-border-radius: 6;");
        noteBox.getChildren().addAll(noteLabel, noteArea);

        // ── Eylem Butonları (NFPA 72 §26.6.3.3) ────────────────────────────
        HBox actionButtons = new HBox(15);
        
        Button btnAck = new Button("✔ Onayla");
        btnAck.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        
        Button btnReject = new Button("✕ Yanlış Alarm");
        btnReject.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-border-color: #ef4444; -fx-border-radius: 8; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        
        Button btnDispatch = new Button("🚒 Ekip Çağır");
        btnDispatch.setStyle("-fx-background-color: #991b1b; -fx-text-fill: #fecaca; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        // İşlevsellik
        btnAck.setOnAction(e -> {
            AlarmController.getInstance().acknowledgeAlarm(alarm, noteArea.getText());
            closeView();
        });

        btnReject.setOnAction(e -> {
            AlarmController.getInstance().rejectAlarm(alarm, noteArea.getText());
            closeView();
        });

        btnDispatch.setOnAction(e -> {
            AlarmController.getInstance().dispatchTeam(alarm);
            btnDispatch.setText("🚒 Ekip Çağırıldı!");
            btnDispatch.setDisable(true);
        });

        // Duruma göre butonları kilitleme
        if (!"Yeni".equals(alarm.getStatus())) {
            btnAck.setDisable(true);
            btnReject.setDisable(true);
            noteArea.setEditable(false);
        }

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        actionButtons.getChildren().addAll(btnAck, btnReject, btnSpacer, btnDispatch);

        card.getChildren().addAll(grid, noteBox, actionButtons);

        // ── Geri Dön ───────────────────────────────────────────────────────
        Button backButton = new Button(onCloseCallback != null ? "✕ Kapat" : "← Geçmişe Dön");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand;");
        backButton.setOnAction(e -> closeView());

        root.getChildren().addAll(backButton, header, card);
        return root;
    }

    private void addRow(GridPane grid, int rowIndex, String label, String value) {
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 14px; -fx-text-fill: #e2e8f0;");

        grid.add(labelNode, 0, rowIndex);
        grid.add(valueNode, 1, rowIndex);
    }
}