package com.gozcu.ui;

import com.gozcu.model.Camera;
import com.gozcu.repository.CameraRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CameraView {

    private final CameraRepository cameraRepository = new CameraRepository();

    public VBox getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Kamera Yönetimi");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Sisteme bağlı kameralar burada eklenir ve listelenir.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        VBox formCard = createCard();

        Label formTitle = new Label("Yeni Kamera Ekle");
        formTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        TextField nameField = new TextField();
        nameField.setPromptText("Kamera adı");

        TextField locationField = new TextField();
        locationField.setPromptText("Konum");

        TextField sourceField = new TextField();
        sourceField.setPromptText("Kamera kaynağı / RTSP URL / test video yolu");

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Aktif", "Pasif");
        statusComboBox.setValue("Aktif");
        statusComboBox.setPrefWidth(150);

        Spinner<Integer> sensitivitySpinner = new Spinner<>(1, 100, 60);
        sensitivitySpinner.setPrefWidth(120);

        Button addButton = new Button("Kamera Ekle");
        addButton.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        Button deleteButton = new Button("Seçili Kamerayı Sil");
        deleteButton.setStyle(
                "-fx-background-color: #dc2626;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        HBox firstRow = new HBox(12, nameField, locationField);
        HBox secondRow = new HBox(12, sourceField, statusComboBox, sensitivitySpinner, addButton, deleteButton);

        firstRow.setAlignment(Pos.CENTER_LEFT);
        secondRow.setAlignment(Pos.CENTER_LEFT);

        formCard.getChildren().addAll(formTitle, firstRow, secondRow);

        TableView<Camera> tableView = createCameraTable();
        ObservableList<Camera> cameraList = FXCollections.observableArrayList(cameraRepository.findAllCameras());
        tableView.setItems(cameraList);

        addButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String location = locationField.getText().trim();
            String source = sourceField.getText().trim();
            String status = statusComboBox.getValue();
            int sensitivity = sensitivitySpinner.getValue();

            if (name.isEmpty() || location.isEmpty() || source.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Eksik Bilgi");
                alert.setHeaderText(null);
                alert.setContentText("Lütfen kamera adı, konum ve kaynak alanlarını doldur.");
                alert.showAndWait();
                return;
            }

            Camera camera = new Camera(name, location, source, status, sensitivity);
            cameraRepository.saveCamera(camera);

            cameraList.setAll(cameraRepository.findAllCameras());

            nameField.clear();
            locationField.clear();
            sourceField.clear();
            statusComboBox.setValue("Aktif");
            sensitivitySpinner.getValueFactory().setValue(60);
        });

        deleteButton.setOnAction(e -> {
            Camera selectedCamera = tableView.getSelectionModel().getSelectedItem();

            if (selectedCamera == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Kamera Seçilmedi");
                alert.setHeaderText(null);
                alert.setContentText("Lütfen silmek istediğin kamerayı tablodan seç.");
                alert.showAndWait();
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Kamera Silme");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText(selectedCamera.getName() + " kamerasını silmek istediğine emin misin?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    cameraRepository.deleteCamera(selectedCamera.getId());
                    cameraList.setAll(cameraRepository.findAllCameras());
                }
            });
        });

        root.getChildren().addAll(title, description, formCard, tableView);

        return root;
    }

    private VBox createCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );
        return card;
    }

    private TableView<Camera> createCameraTable() {
        TableView<Camera> tableView = new TableView<>();
        tableView.setPrefHeight(360);

        TableColumn<Camera, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<Camera, String> nameColumn = new TableColumn<>("Kamera Adı");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(160);

        TableColumn<Camera, String> locationColumn = new TableColumn<>("Konum");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationColumn.setPrefWidth(160);

        TableColumn<Camera, String> sourceColumn = new TableColumn<>("Kaynak");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        sourceColumn.setPrefWidth(260);

        TableColumn<Camera, String> statusColumn = new TableColumn<>("Durum");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        TableColumn<Camera, Integer> sensitivityColumn = new TableColumn<>("Hassasiyet");
        sensitivityColumn.setCellValueFactory(new PropertyValueFactory<>("sensitivity"));
        sensitivityColumn.setPrefWidth(120);

        tableView.getColumns().addAll(
                idColumn,
                nameColumn,
                locationColumn,
                sourceColumn,
                statusColumn,
                sensitivityColumn
        );

        return tableView;
    }
}