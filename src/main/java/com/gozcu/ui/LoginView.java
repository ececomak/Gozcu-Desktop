package com.gozcu.ui;

import com.gozcu.model.Operator;
import com.gozcu.repository.OperatorRepository;
import com.gozcu.util.SessionManager;
import com.gozcu.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {

    private final Stage primaryStage;

    public LoginView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("GÖZCÜ");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Yangın ve Duman Tespit Sistemi\nLütfen giriş yapınız.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");

        VBox card = new VBox(15);
        card.setMaxWidth(350);
        card.setPadding(new Insets(30));
        card.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);"
        );

        Label lblId = new Label("Kurum Sicil No:");
        lblId.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextField txtId = new TextField();
        txtId.setId("txtId"); // TestFX ID
        txtId.setPromptText("Örn: 1234");
        txtId.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: white; -fx-padding: 8;");

        Label lblPass = new Label("Şifre / PIN:");
        lblPass.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        PasswordField txtPass = new PasswordField();
        txtPass.setId("txtPass"); // TestFX ID
        txtPass.setPromptText("Şifre");
        txtPass.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: white; -fx-padding: 8;");

        Label errorLabel = new Label();
        errorLabel.setId("errorLabel"); // TestFX ID
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        Button btnLogin = new Button("Giriş Yap");
        btnLogin.setId("btnLogin"); // TestFX ID
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle(
            "-fx-background-color: #2563eb;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 10;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );

        btnLogin.setOnAction(e -> {
            String empId = txtId.getText().trim();
            String pass = txtPass.getText().trim();

            if (empId.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Lütfen tüm alanları doldurun.");
                errorLabel.setVisible(true);
                return;
            }

            OperatorRepository repo = new OperatorRepository();
            Operator op = repo.findByEmployeeId(empId);

            if (op != null && op.getPasswordHash().equals(pass)) {
                SessionManager.login(op);
                launchMainApp();
            } else {
                errorLabel.setText("Hatalı sicil no veya şifre!");
                errorLabel.setVisible(true);
            }
        });

        card.getChildren().addAll(lblId, txtId, lblPass, txtPass, errorLabel, btnLogin);
        root.getChildren().addAll(title, subtitle, card);

        Scene scene = new Scene(root, 1200, 750);
        ThemeManager.register(scene);
        primaryStage.setScene(scene);
    }

    private void launchMainApp() {
        MainLayout layout = new MainLayout();
        Scene scene = new Scene(layout.getLayout(), 1200, 750);
        ThemeManager.register(scene);
        primaryStage.setScene(scene);
    }
}
