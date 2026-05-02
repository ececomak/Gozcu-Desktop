package com.gozcu.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.gozcu.model.Alarm;
import com.gozcu.repository.AlarmRepository;
import com.gozcu.model.Camera;
import com.gozcu.repository.CameraRepository;
import com.gozcu.util.AppSettings;
import javafx.collections.FXCollections;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LiveMonitorView {

    private final CameraRepository cameraRepository = new CameraRepository();

    private Label statusBadge;
    private Label confidenceValue;
    private ProgressBar confidenceBar;
    private Label lastDetectionText;
    private VBox cameraArea;

    public VBox getView() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Canlı İzleme");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Kamera görüntüsü ve anlık duman tespiti bu ekranda izlenir.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        HBox topControls = new HBox(15);
        topControls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Camera> cameraComboBox = new ComboBox<>();

        cameraComboBox.setItems(FXCollections.observableArrayList(
                cameraRepository.findAllCameras()
        ));

        if (!cameraComboBox.getItems().isEmpty()) {
            cameraComboBox.setValue(cameraComboBox.getItems().get(0));
        }

        cameraComboBox.setPrefWidth(260);

        statusBadge = new Label("SİSTEM NORMAL");
        statusBadge.setStyle(getNormalBadgeStyle());

        Button testAlarmButton = new Button("Test Alarmı Üret");
        testAlarmButton.setStyle(
                "-fx-background-color: #dc2626;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        testAlarmButton.setOnAction(e -> {
            Camera selectedCamera = cameraComboBox.getValue();

            if (selectedCamera == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Kamera Seçilmedi");
                alert.setHeaderText(null);
                alert.setContentText("Lütfen önce Kameralar ekranından bir kamera ekle.");
                alert.showAndWait();
                return;
            }

            simulateSmokeDetection(selectedCamera);
        });
        topControls.getChildren().addAll(cameraComboBox, statusBadge, testAlarmButton);

        HBox mainContent = new HBox(20);

        cameraArea = new VBox(15);
        cameraArea.setAlignment(Pos.CENTER);
        cameraArea.setPrefSize(620, 390);
        cameraArea.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;"
        );

        Label cameraPlaceholder = new Label("Kamera Görüntüsü");
        cameraPlaceholder.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label cameraInfo = new Label("Model entegrasyonu sonrası canlı görüntü burada yer alacak.");
        cameraInfo.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");

        cameraArea.getChildren().addAll(cameraPlaceholder, cameraInfo);

        VBox detectionPanel = createPanel("Tespit Bilgisi");
        detectionPanel.setPrefSize(360, 390);

        Label confidenceTitle = new Label("Güven Oranı");
        confidenceTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        confidenceValue = new Label("%0");
        confidenceValue.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        confidenceBar = new ProgressBar(0);
        confidenceBar.setPrefWidth(280);

        lastDetectionText = new Label("Henüz duman tespiti yapılmadı.");
        lastDetectionText.setWrapText(true);
        lastDetectionText.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        detectionPanel.getChildren().addAll(
                confidenceTitle,
                confidenceValue,
                confidenceBar,
                createInfoRow("Alarm Türü", "-"),
                createInfoRow("Konum", "-"),
                createInfoRow("Son Tespit", "-"),
                lastDetectionText
        );

        mainContent.getChildren().addAll(cameraArea, detectionPanel);

        root.getChildren().addAll(title, description, topControls, mainContent);

        return root;
    }

    private void simulateSmokeDetection(Camera selectedCamera) {
        statusBadge.setText("DUMAN ALGILANDI");
        statusBadge.setStyle(getDangerBadgeStyle());

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        int simulatedConfidence = 87;

        if (simulatedConfidence < AppSettings.getMinimumConfidence()) {
            statusBadge.setText("EŞİK ALTINDA");
            statusBadge.setStyle(getNormalBadgeStyle());

            confidenceValue.setText("%" + simulatedConfidence);
            confidenceBar.setProgress(simulatedConfidence / 100.0);

            lastDetectionText.setText(
                    "Duman benzeri hareket algılandı fakat güven oranı minimum eşiğin altında kaldı.\n" +
                            "Minimum eşik: %" + AppSettings.getMinimumConfidence() + "\n" +
                            "Algılanan güven: %" + simulatedConfidence
            );

            return;
        }
        confidenceValue.setText("%" + simulatedConfidence);
        confidenceBar.setProgress(simulatedConfidence / 100.0);

        lastDetectionText.setText(
                selectedCamera.getLocation() + " kamerasında duman tespiti yapıldı.\n" +
                        "Tespit zamanı: " + now + "\n" +
                        "Bu kayıt sonraki aşamada veritabanına alarm olarak kaydedilecek."
        );

        cameraArea.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-color: #dc2626;" +
                        "-fx-border-width: 4;"
        );

        Alarm alarm = new Alarm(
                selectedCamera.getName(),
                selectedCamera.getLocation(),
                "Duman",
                "Kritik",
                simulatedConfidence / 100.0,
                "Yeni",
                now,
                "Test alarmı"
        );

        new AlarmRepository().saveAlarm(alarm);
    }

    private VBox createPanel(String title) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        panel.getChildren().add(titleLabel);
        return panel;
    }

    private HBox createInfoRow(String title, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title + ":");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-font-weight: bold;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");

        row.getChildren().addAll(titleLabel, valueLabel);
        return row;
    }

    private String getNormalBadgeStyle() {
        return "-fx-background-color: #dcfce7;" +
                "-fx-text-fill: #166534;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 9 16;" +
                "-fx-background-radius: 20;";
    }

    private String getDangerBadgeStyle() {
        return "-fx-background-color: #fee2e2;" +
                "-fx-text-fill: #991b1b;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 9 16;" +
                "-fx-background-radius: 20;";
    }
}