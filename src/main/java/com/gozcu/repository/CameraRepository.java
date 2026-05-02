package com.gozcu.repository;

import com.gozcu.model.Camera;
import com.gozcu.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CameraRepository {

    public void saveCamera(Camera camera) {
        String sql = """
                INSERT INTO cameras (name, location, source, status, sensitivity)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, camera.getName());
            statement.setString(2, camera.getLocation());
            statement.setString(3, camera.getSource());
            statement.setString(4, camera.getStatus());
            statement.setInt(5, camera.getSensitivity());

            statement.executeUpdate();
            System.out.println("Kamera veritabanına kaydedildi.");

        } catch (SQLException e) {
            System.out.println("Kamera kaydedilirken hata: " + e.getMessage());
        }
    }

    public List<Camera> findAllCameras() {
        List<Camera> cameras = new ArrayList<>();

        String sql = """
                SELECT id, name, location, source, status, sensitivity
                FROM cameras
                ORDER BY id DESC
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Camera camera = new Camera(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("location"),
                        resultSet.getString("source"),
                        resultSet.getString("status"),
                        resultSet.getInt("sensitivity")
                );

                cameras.add(camera);
            }

        } catch (SQLException e) {
            System.out.println("Kameralar getirilirken hata: " + e.getMessage());
        }

        return cameras;
    }

    public void deleteCamera(int cameraId) {
        String sql = """
            DELETE FROM cameras
            WHERE id = ?
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, cameraId);
            statement.executeUpdate();

            System.out.println("Kamera silindi.");

        } catch (SQLException e) {
            System.out.println("Kamera silinirken hata: " + e.getMessage());
        }
    }

    public int countActiveCameras() {
        String sql = "SELECT COUNT(*) FROM cameras WHERE status = 'Aktif'";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("Aktif kamera sayısı alınırken hata: " + e.getMessage());
        }

        return 0;
    }
}