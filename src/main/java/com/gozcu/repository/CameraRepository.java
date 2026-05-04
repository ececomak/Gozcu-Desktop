package com.gozcu.repository;

import com.gozcu.model.Camera;
import com.gozcu.util.DatabaseManager;

import java.sql.*;
import java.util.*;

public class CameraRepository {

    // ── INSERT OR IGNORE → UNIQUE index sayesinde mükerrer kayıt oluşmaz ────

    public void saveCamera(Camera camera) {
        String sql = """
            INSERT OR IGNORE INTO cameras (name, location, source, status, sensitivity)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql)) {
            s.setString(1, camera.getName());
            s.setString(2, camera.getLocation());
            s.setString(3, camera.getSource());
            s.setString(4, camera.getStatus());
            s.setInt(5, camera.getSensitivity());
            int rows = s.executeUpdate();
            if (rows > 0) System.out.println("Kamera eklendi: " + camera.getName());
        } catch (SQLException e) {
            System.err.println("saveCamera hata: " + e.getMessage());
        }
    }

    public List<Camera> findAllCameras() {
        List<Camera> list = new ArrayList<>();
        String sql = "SELECT id, name, location, source, status, sensitivity FROM cameras ORDER BY id";
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                list.add(new Camera(rs.getInt("id"), rs.getString("name"),
                        rs.getString("location"), rs.getString("source"),
                        rs.getString("status"), rs.getInt("sensitivity")));
            }
        } catch (SQLException e) {
            System.err.println("findAllCameras hata: " + e.getMessage());
        }
        return list;
    }

    public void updateCamera(Camera camera) {
        String sql = "UPDATE cameras SET name=?, location=?, source=?, status=?, sensitivity=? WHERE id=?";
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql)) {
            s.setString(1, camera.getName());
            s.setString(2, camera.getLocation());
            s.setString(3, camera.getSource());
            s.setString(4, camera.getStatus());
            s.setInt(5, camera.getSensitivity());
            s.setInt(6, camera.getId());
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateCamera hata: " + e.getMessage());
        }
    }

    public void deleteCamera(int cameraId) {
        try (PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement("DELETE FROM cameras WHERE id=?")) {
            s.setInt(1, cameraId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteCamera hata: " + e.getMessage());
        }
    }

    public int countActiveCameras() {
        try (PreparedStatement s = DatabaseManager.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM cameras WHERE status='Aktif'");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("countActiveCameras hata: " + e.getMessage());
        }
        return 0;
    }
}