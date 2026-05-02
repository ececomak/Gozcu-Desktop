package com.gozcu.util;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class SceneManager {

    private static BorderPane mainLayout;

    public static void setMainLayout(BorderPane layout) {
        mainLayout = layout;
    }

    public static void showPage(Pane page) {
        if (mainLayout != null) {
            ScrollPane scrollPane = new ScrollPane(page);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #f4f6f8; -fx-background: #f4f6f8;");
            mainLayout.setCenter(scrollPane);
        }
    }
}