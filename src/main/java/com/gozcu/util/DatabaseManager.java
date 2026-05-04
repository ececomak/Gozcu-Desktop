package com.gozcu.util;

import java.sql.*;

/**
 * Singleton SQLite bağlantısı + WAL modu + UNIQUE index yönetimi.
 * Tüm repository'ler bu sınıftan bağlantı alır — her sorguda yeni bağlantı açılmaz.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:gozcu.db";
    private static Connection conn;

    /** Thread-safe singleton bağlantı döner. */
    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_URL);
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL;");
                s.execute("PRAGMA synchronous=NORMAL;");
                s.execute("PRAGMA cache_size=2000;");
                s.execute("PRAGMA foreign_keys=ON;");
            }
        }
        return conn;
    }

    /** Uygulama kapanırken çağrılır. */
    public static synchronized void closeConnection() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
            conn = null;
        }
    }

    public static void initializeDatabase() {
        try (Statement s = getConnection().createStatement()) {

            s.execute("""
                CREATE TABLE IF NOT EXISTS alarms (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    camera_name    TEXT    NOT NULL,
                    location       TEXT    NOT NULL,
                    alarm_type     TEXT    NOT NULL,
                    level          TEXT    NOT NULL,
                    confidence     REAL    NOT NULL,
                    status         TEXT    NOT NULL DEFAULT 'Yeni',
                    detection_time TEXT    NOT NULL,
                    note           TEXT
                );
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS cameras (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL,
                    location    TEXT    NOT NULL,
                    source      TEXT    NOT NULL,
                    status      TEXT    NOT NULL DEFAULT 'Aktif',
                    sensitivity INTEGER NOT NULL DEFAULT 60
                );
            """);

            // source sütununda UNIQUE index — mükerrer webcam kaydını engeller
            s.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_cameras_source ON cameras(source);");

            System.out.println("Veritabanı hazır.");

        } catch (SQLException e) {
            System.err.println("Veritabanı hatası: " + e.getMessage());
        }
    }
}