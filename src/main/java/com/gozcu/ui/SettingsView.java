package com.gozcu.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.gozcu.util.AppSettings;
import com.gozcu.util.ThemeManager;

public class SettingsView {

    public VBox getView() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Ayarlar");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Bildirim, alarm sesi ve algılama eşiği ayarları burada yönetilir.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        VBox card = new VBox(18);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Label cardTitle = new Label("Uygulama Ayarları");
        cardTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        CheckBox notificationCheckBox = new CheckBox("Duman tespiti olduğunda bildirim göster");
        notificationCheckBox.setSelected(AppSettings.isNotificationsEnabled());

        CheckBox soundCheckBox = new CheckBox("Alarm sesi aktif olsun");
        soundCheckBox.setSelected(AppSettings.isSoundEnabled());

        Label thresholdLabel = new Label("Minimum güven eşiği: %" + AppSettings.getMinimumConfidence());
        thresholdLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Slider thresholdSlider = new Slider(0, 100, AppSettings.getMinimumConfidence());
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(20);
        thresholdSlider.setBlockIncrement(5);
        thresholdSlider.setPrefWidth(350);

        thresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int value = newValue.intValue();
            thresholdLabel.setText("Minimum güven eşiği: %" + value);
        });

        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("Açık Tema", "Koyu Tema");
        themeComboBox.setValue(AppSettings.getTheme());
        themeComboBox.setPrefWidth(180);

        HBox themeRow = new HBox(12);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        Label themeLabel = new Label("Tema:");
        themeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        themeRow.getChildren().addAll(themeLabel, themeComboBox);

        HBox operatorRow = new HBox(12);
        operatorRow.setAlignment(Pos.CENTER_LEFT);
        Label operatorLabel = new Label("Operatör Adı:");
        operatorLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        TextField operatorField = new TextField(AppSettings.getOperatorName());
        operatorField.setPrefWidth(200);
        operatorRow.getChildren().addAll(operatorLabel, operatorField);

        Button saveButton = new Button("Ayarları Kaydet");
        saveButton.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;");

        saveButton.setOnAction(e -> {
            AppSettings.setNotificationsEnabled(notificationCheckBox.isSelected());
            AppSettings.setSoundEnabled(soundCheckBox.isSelected());
            AppSettings.setMinimumConfidence((int) thresholdSlider.getValue());
            AppSettings.setOperatorName(operatorField.getText());
            ThemeManager.switchTheme(themeComboBox.getValue()); // tema anında değişir + kaydeder

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ayarlar Kaydedildi");
            alert.setHeaderText(null);
            alert.setContentText("Ayarlar kaydedildi. Tema değişikliği anlık uygulandı.");
            alert.showAndWait();
        });

        card.getChildren().addAll(
                cardTitle,
                notificationCheckBox,
                soundCheckBox,
                thresholdLabel,
                thresholdSlider,
                themeRow,
                operatorRow,
                saveButton);

        root.getChildren().addAll(title, description, card);

        return root;
    }
}