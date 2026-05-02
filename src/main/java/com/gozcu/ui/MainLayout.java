package com.gozcu.ui;

import com.gozcu.util.SceneManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainLayout {

    public BorderPane getLayout() {
        BorderPane root = new BorderPane();

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(25));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #111827;");

        Label appTitle = new Label("GÖZCÜ");
        appTitle.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtitle = new Label("Yangın Tespit Paneli");
        subtitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");

        Button dashboardButton = createMenuButton("Dashboard");
        Button liveButton = createMenuButton("Canlı İzleme");
        Button historyButton = createMenuButton("Alarm Geçmişi");
        Button cameraButton = createMenuButton("Kameralar");
        Button settingsButton = createMenuButton("Ayarlar");
        Button exitButton = createMenuButton("Çıkış");

        dashboardButton.setOnAction(e -> SceneManager.showPage(new DashboardView().getView()));
        liveButton.setOnAction(e -> SceneManager.showPage(new LiveMonitorView().getView()));
        historyButton.setOnAction(e -> SceneManager.showPage(new AlarmHistoryView().getView()));
        cameraButton.setOnAction(e -> SceneManager.showPage(new CameraView().getView()));
        settingsButton.setOnAction(e -> SceneManager.showPage(new SettingsView().getView()));
        exitButton.setOnAction(e -> System.exit(0));

        sidebar.getChildren().addAll(
                appTitle,
                subtitle,
                dashboardButton,
                liveButton,
                historyButton,
                cameraButton,
                settingsButton,
                exitButton
        );

        root.setLeft(sidebar);
        SceneManager.setMainLayout(root);
        SceneManager.showPage(new DashboardView().getView());

        return root;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(170);
        button.setPrefHeight(42);
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-alignment: center-left;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-alignment: center-left;" +
                        "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-alignment: center-left;" +
                        "-fx-cursor: hand;"
        ));

        return button;
    }
}