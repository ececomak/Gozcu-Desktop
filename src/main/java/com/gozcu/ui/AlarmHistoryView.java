package com.gozcu.ui;

import com.gozcu.model.Alarm;
import com.gozcu.repository.AlarmRepository;
import com.gozcu.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

public class AlarmHistoryView {

    private final AlarmRepository alarmRepository = new AlarmRepository();

    public VBox getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Alarm Geçmişi");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label description = new Label("Kaydedilen duman tespit bildirimleri burada listelenir.");
        description.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        TextField searchField = new TextField();
        searchField.setPromptText("Kamera veya konum ara...");
        searchField.setPrefWidth(260);

        ComboBox<String> levelFilter = new ComboBox<>();
        levelFilter.getItems().addAll("Tümü", "Kritik", "Orta", "Düşük");
        levelFilter.setValue("Tümü");
        levelFilter.setPrefWidth(140);

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tümü", "Yeni", "İncelendi", "Yanlış Alarm", "Gerçek Alarm");
        statusFilter.setValue("Tümü");
        statusFilter.setPrefWidth(160);

        Button deleteButton = new Button("Seçili Alarmı Sil");
        deleteButton.setStyle(
                "-fx-background-color: #dc2626;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 14;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(searchField, levelFilter, statusFilter, deleteButton);

        TableView<Alarm> tableView = new TableView<>();
        tableView.setPrefHeight(500);
        tableView.setStyle("-fx-background-color: white; -fx-background-radius: 14;");

        TableColumn<Alarm, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<Alarm, String> timeColumn = new TableColumn<>("Tarih-Saat");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("detectionTime"));
        timeColumn.setPrefWidth(170);

        TableColumn<Alarm, String> cameraColumn = new TableColumn<>("Kamera");
        cameraColumn.setCellValueFactory(new PropertyValueFactory<>("cameraName"));
        cameraColumn.setPrefWidth(130);

        TableColumn<Alarm, String> locationColumn = new TableColumn<>("Konum");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationColumn.setPrefWidth(140);

        TableColumn<Alarm, String> typeColumn = new TableColumn<>("Tür");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("alarmType"));
        typeColumn.setPrefWidth(100);

        TableColumn<Alarm, String> levelColumn = new TableColumn<>("Seviye");
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelColumn.setPrefWidth(100);

        TableColumn<Alarm, Double> confidenceColumn = new TableColumn<>("Güven");
        confidenceColumn.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        confidenceColumn.setPrefWidth(100);

        TableColumn<Alarm, String> statusColumn = new TableColumn<>("Durum");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(130);

        TableColumn<Alarm, Void> actionColumn = new TableColumn<>("Aksiyon");
        actionColumn.setPrefWidth(100);
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Detay →");
            {
                btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 10; -fx-background-radius: 4;");
                btn.setOnAction(e -> {
                    Alarm alarm = getTableView().getItems().get(getIndex());
                    SceneManager.showPage(new AlarmDetailView(alarm).getView());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        tableView.getColumns().addAll(
                idColumn,
                timeColumn,
                cameraColumn,
                locationColumn,
                typeColumn,
                levelColumn,
                confidenceColumn,
                statusColumn,
                actionColumn
        );

        // Kritik alarmları vurgulamak için RowFactory
        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Alarm item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("Kritik".equals(item.getLevel())) {
                    setStyle("-fx-background-color: #450a0a;"); // Koyu kırmızımsı arka plan
                } else {
                    setStyle("");
                }
            }
        });

        ObservableList<Alarm> alarmList = FXCollections.observableArrayList(
                alarmRepository.findAllAlarms()
        );

        FilteredList<Alarm> filteredList = new FilteredList<>(alarmList, alarm -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                applyFilters(filteredList, alarmList, searchField, levelFilter, statusFilter)
        );

        levelFilter.valueProperty().addListener((observable, oldValue, newValue) ->
                applyFilters(filteredList, alarmList, searchField, levelFilter, statusFilter)
        );

        statusFilter.valueProperty().addListener((observable, oldValue, newValue) ->
                applyFilters(filteredList, alarmList, searchField, levelFilter, statusFilter)
        );

        tableView.setItems(filteredList);

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                Alarm selectedAlarm = tableView.getSelectionModel().getSelectedItem();
                SceneManager.showPage(new AlarmDetailView(selectedAlarm).getView());
            }
        });

        deleteButton.setOnAction(e -> {
            Alarm selectedAlarm = tableView.getSelectionModel().getSelectedItem();

            if (selectedAlarm == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Alarm Seçilmedi");
                alert.setHeaderText(null);
                alert.setContentText("Lütfen silmek istediğin alarmı tablodan seç.");
                alert.showAndWait();
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Alarm Silme");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Seçili alarm kaydını silmek istediğine emin misin?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    alarmRepository.deleteAlarm(selectedAlarm.getId());

                    alarmList.setAll(alarmRepository.findAllAlarms());

                    applyFilters(filteredList, alarmList, searchField, levelFilter, statusFilter);
                }
            });
        });

        root.getChildren().addAll(title, description, filterBar, tableView);

        return root;
    }

    private void applyFilters(
            FilteredList<Alarm> filteredList,
            ObservableList<Alarm> alarmList,
            TextField searchField,
            ComboBox<String> levelFilter,
            ComboBox<String> statusFilter
    ) {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedLevel = levelFilter.getValue();
        String selectedStatus = statusFilter.getValue();

        filteredList.setPredicate(alarm -> {
            boolean matchesSearch =
                    alarm.getCameraName().toLowerCase().contains(searchText) ||
                            alarm.getLocation().toLowerCase().contains(searchText);

            boolean matchesLevel =
                    selectedLevel.equals("Tümü") ||
                            alarm.getLevel().equals(selectedLevel);

            boolean matchesStatus =
                    selectedStatus.equals("Tümü") ||
                            alarm.getStatus().equals(selectedStatus);

            return matchesSearch && matchesLevel && matchesStatus;
        });
    }
}