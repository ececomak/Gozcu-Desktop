package com.gozcu.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.gozcu.model.Alarm;
import com.gozcu.repository.AlarmRepository;
import com.gozcu.repository.CameraRepository;

public class DashboardView {

    private final AlarmRepository alarmRepository = new AlarmRepository();
    private final CameraRepository cameraRepository = new CameraRepository();

    public VBox getView() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Gözcü Desktop ana kontrol paneli.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        FlowPane cardArea = new FlowPane();
        cardArea.setHgap(20);
        cardArea.setVgap(20);

        int activeCameraCount = cameraRepository.countActiveCameras();
        int todayAlarmCount = alarmRepository.countTodayAlarms();
        int criticalAlarmCount = alarmRepository.countCriticalAlarms();
        Alarm lastAlarm = alarmRepository.findLastAlarm();

        String lastAlarmValue = "-";
        String lastAlarmSubtitle = "Henüz alarm oluşmadı";

        if (lastAlarm != null) {
            String detectionTime = lastAlarm.getDetectionTime();

            if (detectionTime != null && detectionTime.length() >= 16) {
                lastAlarmValue = detectionTime.substring(11, 16);
            } else {
                lastAlarmValue = detectionTime;
            }

            lastAlarmSubtitle = lastAlarm.getLocation() + " alanında " + lastAlarm.getAlarmType().toLowerCase() + " tespiti";
        }

        cardArea.getChildren().addAll(
                createInfoCard("Aktif Kamera", String.valueOf(activeCameraCount), "Sisteme bağlı aktif kamera sayısı"),
                createInfoCard("Bugünkü Alarm", String.valueOf(todayAlarmCount), "Bugün oluşan toplam alarm"),
                createInfoCard("Kritik Alarm", String.valueOf(criticalAlarmCount), "Yüksek riskli duman tespiti"),
                createInfoCard("Son Alarm", lastAlarmValue, lastAlarmSubtitle)
        );

        HBox contentArea = new HBox(20);

        VBox cameraPreview = createPanel("Canlı Kamera Önizleme");
        cameraPreview.setPrefSize(520, 310);

        Label statusBadge = new Label("SİSTEM NORMAL");
        statusBadge.setStyle(
                "-fx-background-color: #dcfce7;" +
                        "-fx-text-fill: #166534;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 14;" +
                        "-fx-background-radius: 20;"
        );

        Label cameraText = new Label(
                "Kamera görüntüsü burada gösterilecek.\n\n" +
                        "Model entegrasyonu yapıldığında duman tespiti, güven oranı ve alarm durumu bu alanda gösterilecek."
        );
        cameraText.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");
        cameraText.setWrapText(true);

        cameraPreview.getChildren().addAll(statusBadge, cameraText);

        VBox recentAlarms = createPanel("Son Alarmlar");
        recentAlarms.setPrefSize(380, 310);

        java.util.List<Alarm> recentAlarmList = alarmRepository.findRecentAlarms(3);

        if (recentAlarmList.isEmpty()) {
            Label emptyLabel = new Label("Henüz kayıtlı alarm yok.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
            recentAlarms.getChildren().add(emptyLabel);
        } else {
            for (Alarm alarm : recentAlarmList) {
                String time = "-";

                if (alarm.getDetectionTime() != null && alarm.getDetectionTime().length() >= 16) {
                    time = alarm.getDetectionTime().substring(11, 16);
                }

                recentAlarms.getChildren().add(
                        createAlarmItem(time, alarm.getLocation(), alarm.getAlarmType(), alarm.getLevel())
                );
            }
        }

        contentArea.getChildren().addAll(cameraPreview, recentAlarms);

        root.getChildren().addAll(title, description, cardArea, contentArea);

        return root;
    }

    private VBox createInfoCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefSize(220, 120);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
        subtitleLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    private VBox createPanel(String title) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        panel.getChildren().add(titleLabel);
        return panel;
    }

    private HBox createAlarmItem(String time, String location, String type, String level) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(10));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10;");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        Label infoLabel = new Label(location + " • " + type);
        infoLabel.setStyle("-fx-text-fill: #4b5563;");

        Label levelLabel = new Label(level);
        levelLabel.setStyle(getLevelStyle(level));

        item.getChildren().addAll(timeLabel, infoLabel, levelLabel);
        return item;
    }

    private String getLevelStyle(String level) {
        if (level.equals("Kritik")) {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 20;";
        }
        if (level.equals("Orta")) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #9a3412; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 20;";
        }
        return "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 20;";
    }
}