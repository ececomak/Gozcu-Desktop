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

            // Yeni sütunlar (varsa hata vermez çünkü try-catch ile eklenebilir veya SQLite ALTER kullanımı)
            try { s.execute("ALTER TABLE alarms ADD COLUMN acknowledged_by TEXT;"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE alarms ADD COLUMN acknowledged_at TEXT;"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE alarms ADD COLUMN cap_message_id TEXT;"); } catch (SQLException ignored) {}

            s.execute("""
                CREATE TABLE IF NOT EXISTS alarm_state_log (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    alarm_id    INTEGER NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
                    from_status TEXT    NOT NULL,
                    to_status   TEXT    NOT NULL,
                    changed_by  TEXT    NOT NULL,
                    changed_at  TEXT    NOT NULL,
                    reason      TEXT
                );
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS notification_log (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    alarm_id        INTEGER NOT NULL REFERENCES alarms(id) ON DELETE SET NULL,
                    channel         TEXT    NOT NULL,
                    recipient       TEXT    NOT NULL,
                    message_preview TEXT,
                    sent_at         TEXT    NOT NULL,
                    success         INTEGER NOT NULL DEFAULT 1,
                    error_detail    TEXT
                );
            """);

            System.out.println("Veritabanı hazır.");

        } catch (SQLException e) {
            System.err.println("Veritabanı hatası: " + e.getMessage());
        }
    }
}