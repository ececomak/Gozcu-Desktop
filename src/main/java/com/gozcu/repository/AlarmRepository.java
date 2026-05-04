package com.gozcu.repository;

import com.gozcu.model.Alarm;
import com.gozcu.util.DatabaseManager;

import java.sql.*;
import java.util.*;

public class AlarmRepository {

    private static Alarm mapRow(ResultSet rs) throws SQLException {
        return new Alarm(
                rs.getInt("id"), rs.getString("camera_name"), rs.getString("location"),
                rs.getString("alarm_type"), rs.getString("level"), rs.getDouble("confidence"),
                rs.getString("status"), rs.getString("detection_time"), rs.getString("note")
        );
    }

    public void saveAlarm(Alarm alarm) {
        String sql = """
            INSERT INTO alarms (camera_name, location, alarm_type, level, confidence, status, detection_time, note)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(sql)) {
            s.setString(1, alarm.getCameraName()); s.setString(2, alarm.getLocation());
            s.setString(3, alarm.getAlarmType());  s.setString(4, alarm.getLevel());
            s.setDouble(5, alarm.getConfidence()); s.setString(6, alarm.getStatus());
            s.setString(7, alarm.getDetectionTime()); s.setString(8, alarm.getNote());
            s.executeUpdate();
        } catch (SQLException e) { System.err.println("saveAlarm hata: " + e.getMessage()); }
    }

    public List<Alarm> findAllAlarms() {
        List<Alarm> list = new ArrayList<>();
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM alarms ORDER BY id DESC");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("findAllAlarms hata: " + e.getMessage()); }
        return list;
    }

    public List<Alarm> findRecentAlarms(int limit) {
        List<Alarm> list = new ArrayList<>();
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM alarms ORDER BY id DESC LIMIT ?")) {
            s.setInt(1, limit);
            try (ResultSet rs = s.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { System.err.println("findRecentAlarms hata: " + e.getMessage()); }
        return list;
    }

    public Alarm findLastAlarm() {
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM alarms ORDER BY id DESC LIMIT 1");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("findLastAlarm hata: " + e.getMessage()); }
        return null;
    }

    public void updateAlarmStatus(int alarmId, String newStatus) {
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "UPDATE alarms SET status=? WHERE id=?")) {
            s.setString(1, newStatus); s.setInt(2, alarmId);
            s.executeUpdate();
        } catch (SQLException e) { System.err.println("updateAlarmStatus hata: " + e.getMessage()); }
    }

    public void deleteAlarm(int alarmId) {
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM alarms WHERE id=?")) {
            s.setInt(1, alarmId);
            s.executeUpdate();
        } catch (SQLException e) { System.err.println("deleteAlarm hata: " + e.getMessage()); }
    }

    public int countTodayAlarms() {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM alarms WHERE detection_time LIKE ?")) {
            s.setString(1, today + "%");
            try (ResultSet rs = s.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("countTodayAlarms hata: " + e.getMessage()); }
        return 0;
    }

    public int countCriticalAlarms() {
        try (PreparedStatement s = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM alarms WHERE level='Kritik'");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("countCriticalAlarms hata: " + e.getMessage()); }
        return 0;
    }
}