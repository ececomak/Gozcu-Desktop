package com.gozcu;

import com.gozcu.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.gozcu.util.DatabaseManager;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseManager.initializeDatabase();
        MainLayout mainLayout = new MainLayout();

        Scene scene = new Scene(mainLayout.getLayout(), 1100, 700);

        stage.setTitle("Gözcü Desktop");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}