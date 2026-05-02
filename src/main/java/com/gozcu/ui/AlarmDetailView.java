package com.gozcu.ui;

import com.gozcu.model.Alarm;
import com.gozcu.util.SceneManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.gozcu.repository.AlarmRepository;
import javafx.scene.control.ComboBox;

public class AlarmDetailView {

    private final Alarm alarm;

    private final AlarmRepository alarmRepository = new AlarmRepository();

    public AlarmDetailView(Alarm alarm) {
        this.alarm = alarm;
    }

    public VBox getView() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Alarm Detayı");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Seçilen alarm kaydına ait detay bilgiler.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        VBox card = new VBox(18);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        Label cardTitle = new Label("Alarm Bilgileri");
        cardTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(16);

        addRow(grid, 0, "Alarm ID", String.valueOf(alarm.getId()));
        addRow(grid, 1, "Kamera", alarm.getCameraName());
        addRow(grid, 2, "Konum", alarm.getLocation());
        addRow(grid, 3, "Alarm Türü", alarm.getAlarmType());
        addRow(grid, 4, "Seviye", alarm.getLevel());
        addRow(grid, 5, "Güven Oranı", "%" + (int) (alarm.getConfidence() * 100));
        Label statusLabel = new Label("Durum:");
        statusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Yeni", "İncelendi", "Yanlış Alarm", "Gerçek Alarm");
        statusComboBox.setValue(alarm.getStatus());
        statusComboBox.setPrefWidth(180);

        grid.add(statusLabel, 0, 6);
        grid.add(statusComboBox, 1, 6);
        addRow(grid, 7, "Tarih-Saat", alarm.getDetectionTime());
        addRow(grid, 8, "Not", alarm.getNote() == null ? "-" : alarm.getNote());

        Button saveButton = new Button("Durumu Kaydet");
        saveButton.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        saveButton.setOnAction(e -> {
            String selectedStatus = statusComboBox.getValue();
            alarmRepository.updateAlarmStatus(alarm.getId(), selectedStatus);
            SceneManager.showPage(new AlarmHistoryView().getView());
        });
        Button backButton = new Button("Alarm Geçmişine Dön");
        backButton.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        backButton.setOnAction(e -> SceneManager.showPage(new AlarmHistoryView().getView()));

        card.getChildren().addAll(cardTitle, grid, saveButton, backButton);
        root.getChildren().addAll(title, description, card);

        return root;
    }

    private void addRow(GridPane grid, int rowIndex, String label, String value) {
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 15px; -fx-text-fill: #111827;");
        valueNode.setWrapText(true);

        grid.add(labelNode, 0, rowIndex);
        grid.add(valueNode, 1, rowIndex);
    }
}