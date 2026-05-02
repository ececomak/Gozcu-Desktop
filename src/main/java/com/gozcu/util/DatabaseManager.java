package com.gozcu.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:gozcu.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        String createAlarmTable = """
                CREATE TABLE IF NOT EXISTS alarms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    camera_name TEXT NOT NULL,
                    location TEXT NOT NULL,
                    alarm_type TEXT NOT NULL,
                    level TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    status TEXT NOT NULL,
                    detection_time TEXT NOT NULL,
                    note TEXT
                );
                """;

        String createCameraTable = """
        CREATE TABLE IF NOT EXISTS cameras (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            location TEXT NOT NULL,
            source TEXT NOT NULL,
            status TEXT NOT NULL,
            sensitivity INTEGER NOT NULL
        );
        """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createAlarmTable);
            statement.execute(createCameraTable);
            System.out.println("Veritabanı hazır.");

        } catch (SQLException e) {
            System.out.println("Veritabanı oluşturulurken hata oluştu: " + e.getMessage());
        }
    }
}