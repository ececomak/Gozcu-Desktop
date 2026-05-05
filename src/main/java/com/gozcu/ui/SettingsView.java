package com.gozcu.ui;

import com.gozcu.model.Operator;
import com.gozcu.repository.OperatorRepository;
import com.gozcu.util.AppSettings;
import com.gozcu.util.SessionManager;
import com.gozcu.util.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingsView {

    private final OperatorRepository operatorRepo = new OperatorRepository();

    public VBox getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Sistem Ayarları");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        Label subtitle = new Label("NFPA 72 Uyumluluk ve Operatör Yönetimi");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().add(titleBox);

        TabPane tabPane = new TabPane();
        tabPane.setId("settingsTabPane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
                createSystemTab(),
                createNotificationTab(),
                createOperatorTab()
        );

        root.getChildren().addAll(header, tabPane);
        return root;
    }

    // ── 1. Sistem ve Eşik Ayarları ──────────────────────────────────────────

    private Tab createSystemTab() {
        Tab tab = new Tab("🎛 Sistem ve Sensörler");
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 0 8 8 8;");

        Label sectionTitle = new Label("Sensör Eşik Değerleri");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label thresholdLabel = new Label("Yapay Zeka Güven Eşiği: %" + AppSettings.getMinimumConfidence());
        thresholdLabel.setStyle("-fx-text-fill: #cbd5e1;");

        Slider thresholdSlider = new Slider(0, 100, AppSettings.getMinimumConfidence());
        thresholdSlider.setId("thresholdSlider");
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(20);
        thresholdSlider.setBlockIncrement(5);
        thresholdSlider.setPrefWidth(400);
        
        thresholdSlider.valueProperty().addListener((obs, oldV, newV) -> {
            thresholdLabel.setText("Yapay Zeka Güven Eşiği: %" + newV.intValue());
        });

        Label themeLabel = new Label("Arayüz Teması:");
        themeLabel.setStyle("-fx-text-fill: #cbd5e1;");
        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.setId("themeBox");
        themeBox.getItems().addAll("Açık Tema", "Koyu Tema");
        themeBox.setValue(AppSettings.getTheme());

        Button saveBtn = new Button("Sistem Ayarlarını Kaydet");
        saveBtn.setId("saveSystemBtn");
        saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");
        saveBtn.setOnAction(e -> {
            AppSettings.setMinimumConfidence((int) thresholdSlider.getValue());
            AppSettings.setTheme(themeBox.getValue());
            AppSettings.save();
            ThemeManager.switchTheme(themeBox.getValue());
            showAlert("Başarılı", "Sistem ayarları kaydedildi.");
        });

        content.getChildren().addAll(sectionTitle, thresholdLabel, thresholdSlider, themeLabel, themeBox, saveBtn);
        tab.setContent(content);
        return tab;
    }

    // ── 2. Ses ve Bildirim Ayarları ─────────────────────────────────────────

    private Tab createNotificationTab() {
        Tab tab = new Tab("🔊 Ses ve Bildirimler");
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 0 8 8 8;");

        Label sectionTitle = new Label("NFPA 72 Uyarı Bildirimleri");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        CheckBox soundCheck = new CheckBox("Kritik Durumlarda Tahliye Sesi Çal (Temporal-3)");
        soundCheck.setId("soundCheck");
        soundCheck.setStyle("-fx-text-fill: #cbd5e1;");
        soundCheck.setSelected(AppSettings.isSoundEnabled());

        CheckBox notifCheck = new CheckBox("Duman/Ateş tespitinde arayüz uyarıları göster");
        notifCheck.setId("notifCheck");
        notifCheck.setStyle("-fx-text-fill: #cbd5e1;");
        notifCheck.setSelected(AppSettings.isNotificationsEnabled());

        Button saveBtn = new Button("Bildirim Ayarlarını Kaydet");
        saveBtn.setId("saveNotifBtn");
        saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");
        saveBtn.setOnAction(e -> {
            AppSettings.setSoundEnabled(soundCheck.isSelected());
            AppSettings.setNotificationsEnabled(notifCheck.isSelected());
            AppSettings.save();
            showAlert("Başarılı", "Bildirim ayarları kaydedildi.");
        });

        content.getChildren().addAll(sectionTitle, soundCheck, notifCheck, saveBtn);
        tab.setContent(content);
        return tab;
    }

    // ── 3. Operatör Yönetimi ────────────────────────────────────────────────

    private Tab createOperatorTab() {
        Tab tab = new Tab("👥 Operatör Yönetimi");
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 0 8 8 8;");

        if (!SessionManager.isAdmin()) {
            Label noAuth = new Label("Bu sayfayı görüntülemek için Yönetici (Admin) yetkisine sahip olmalısınız.");
            noAuth.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 16px;");
            content.getChildren().add(noAuth);
            tab.setContent(content);
            return tab;
        }

        Label sectionTitle = new Label("Yetkili Operatörler");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TableView<Operator> table = new TableView<>();
        table.setPrefHeight(250);

        TableColumn<Operator, String> colId = new TableColumn<>("Sicil No");
        colId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colId.setPrefWidth(100);

        TableColumn<Operator, String> colName = new TableColumn<>("Ad Soyad");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<Operator, String> colRole = new TableColumn<>("Rol");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(120);

        TableColumn<Operator, String> colDate = new TableColumn<>("Kayıt Tarihi");
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colDate.setPrefWidth(180);

        table.getColumns().addAll(colId, colName, colRole, colDate);
        ObservableList<Operator> data = FXCollections.observableArrayList(operatorRepo.findAll());
        table.setItems(data);

        // Yeni operatör ekleme formu
        GridPane form = new GridPane();
        form.setHgap(15); form.setVgap(15);

        TextField txtId = new TextField(); txtId.setId("txtOpId"); txtId.setPromptText("Sicil No");
        TextField txtName = new TextField(); txtName.setId("txtOpName"); txtName.setPromptText("Ad Soyad");
        PasswordField txtPass = new PasswordField(); txtPass.setId("txtOpPass"); txtPass.setPromptText("Şifre");
        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.setId("cmbRole");
        cmbRole.getItems().addAll("Operator", "Admin");
        cmbRole.setValue("Operator");

        Button btnAdd = new Button("Operatör Ekle");
        btnAdd.setId("btnAddOp");
        btnAdd.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAdd.setOnAction(e -> {
            if (txtId.getText().isEmpty() || txtName.getText().isEmpty() || txtPass.getText().isEmpty()) {
                showAlert("Hata", "Lütfen tüm alanları doldurun.");
                return;
            }
            Operator newOp = new Operator(txtName.getText(), txtId.getText(), txtPass.getText(), cmbRole.getValue());
            if (operatorRepo.save(newOp)) {
                data.setAll(operatorRepo.findAll());
                txtId.clear(); txtName.clear(); txtPass.clear();
                showAlert("Başarılı", "Yeni operatör eklendi.");
            } else {
                showAlert("Hata", "Operatör eklenemedi. Sicil no benzersiz olmalıdır.");
            }
        });

        form.addRow(0, new Label("Sicil No:"), txtId, new Label("Ad Soyad:"), txtName);
        form.addRow(1, new Label("Şifre:"), txtPass, new Label("Yetki:"), cmbRole);
        form.add(btnAdd, 3, 2);

        // Stil
        form.getChildren().forEach(n -> {
            if (n instanceof Label) ((Label)n).setStyle("-fx-text-fill: #cbd5e1;");
        });

        content.getChildren().addAll(sectionTitle, table, new Separator(), new Label("Yeni Operatör Kaydı"), form);
        tab.setContent(content);
        return tab;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}