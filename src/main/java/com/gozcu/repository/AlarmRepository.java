package com.gozcu.repository;

import com.gozcu.model.Alarm;
import com.gozcu.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlarmRepository {

    public void saveAlarm(Alarm alarm) {
        String sql = """
                INSERT INTO alarms (camera_name, location, alarm_type, level, confidence, status, detection_time, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, alarm.getCameraName());
            statement.setString(2, alarm.getLocation());
            statement.setString(3, alarm.getAlarmType());
            statement.setString(4, alarm.getLevel());
            statement.setDouble(5, alarm.getConfidence());
            statement.setString(6, alarm.getStatus());
            statement.setString(7, alarm.getDetectionTime());
            statement.setString(8, alarm.getNote());

            statement.executeUpdate();
            System.out.println("Alarm veritabanına kaydedildi.");

        } catch (SQLException e) {
            System.out.println("Alarm kaydedilirken hata: " + e.getMessage());
        }
    }

    public List<Alarm> findAllAlarms() {
        List<Alarm> alarms = new ArrayList<>();

        String sql = """
                SELECT id, camera_name, location, alarm_type, level, confidence, status, detection_time, note
                FROM alarms
                ORDER BY id DESC
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Alarm alarm = new Alarm(
                        resultSet.getInt("id"),
                        resultSet.getString("camera_name"),
                        resultSet.getString("location"),
                        resultSet.getString("alarm_type"),
                        resultSet.getString("level"),
                        resultSet.getDouble("confidence"),
                        resultSet.getString("status"),
                        resultSet.getString("detection_time"),
                        resultSet.getString("note")
                );

                alarms.add(alarm);
            }

        } catch (SQLException e) {
            System.out.println("Alarmlar getirilirken hata: " + e.getMessage());
        }

        return alarms;
    }

    public void updateAlarmStatus(int alarmId, String newStatus) {
        String sql = """
            UPDATE alarms
            SET status = ?
            WHERE id = ?
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setInt(2, alarmId);

            statement.executeUpdate();

            System.out.println("Alarm durumu güncellendi.");

        } catch (SQLException e) {
            System.out.println("Alarm durumu güncellenirken hata: " + e.getMessage());
        }
    }

    public void deleteAlarm(int alarmId) {
        String sql = """
            DELETE FROM alarms
            WHERE id = ?
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, alarmId);
            statement.executeUpdate();

            System.out.println("Alarm silindi.");

        } catch (SQLException e) {
            System.out.println("Alarm silinirken hata: " + e.getMessage());
        }
    }

    public int countTodayAlarms() {
        String sql = """
            SELECT COUNT(*)
            FROM alarms
            WHERE detection_time LIKE ?
            """;

        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, today + "%");

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("Bugünkü alarm sayısı alınırken hata: " + e.getMessage());
        }

        return 0;
    }

    public int countCriticalAlarms() {
        String sql = "SELECT COUNT(*) FROM alarms WHERE level = 'Kritik'";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("Kritik alarm sayısı alınırken hata: " + e.getMessage());
        }

        return 0;
    }

    public Alarm findLastAlarm() {
        String sql = """
            SELECT id, camera_name, location, alarm_type, level, confidence, status, detection_time, note
            FROM alarms
            ORDER BY id DESC
            LIMIT 1
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return new Alarm(
                        resultSet.getInt("id"),
                        resultSet.getString("camera_name"),
                        resultSet.getString("location"),
                        resultSet.getString("alarm_type"),
                        resultSet.getString("level"),
                        resultSet.getDouble("confidence"),
                        resultSet.getString("status"),
                        resultSet.getString("detection_time"),
                        resultSet.getString("note")
                );
            }

        } catch (SQLException e) {
            System.out.println("Son alarm alınırken hata: " + e.getMessage());
        }

        return null;
    }

    public List<Alarm> findRecentAlarms(int limit) {
        List<Alarm> alarms = new ArrayList<>();

        String sql = """
            SELECT id, camera_name, location, alarm_type, level, confidence, status, detection_time, note
            FROM alarms
            ORDER BY id DESC
            LIMIT ?
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    alarms.add(new Alarm(
                            resultSet.getInt("id"),
                            resultSet.getString("camera_name"),
                            resultSet.getString("location"),
                            resultSet.getString("alarm_type"),
                            resultSet.getString("level"),
                            resultSet.getDouble("confidence"),
                            resultSet.getString("status"),
                            resultSet.getString("detection_time"),
                            resultSet.getString("note")
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println("Son alarmlar alınırken hata: " + e.getMessage());
        }

        return alarms;
    }
}