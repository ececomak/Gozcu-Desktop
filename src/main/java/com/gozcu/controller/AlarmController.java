package com.gozcu.controller;

import com.gozcu.model.Alarm;
import com.gozcu.repository.AlarmRepository;
import com.gozcu.util.AlertSoundPlayer;
import com.gozcu.util.DatabaseManager;
import com.gozcu.util.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Alarm onay/red/ekip çağırma gibi iş mantığını (Business Logic) yöneten merkez.
 */
public class AlarmController {

    private static final Logger LOG = Logger.getLogger(AlarmController.class.getName());
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static AlarmController instance;
    private final AlarmRepository alarmRepo = new AlarmRepository();

    private AlarmController() {}

    public static synchronized AlarmController getInstance() {
        if (instance == null) {
            instance = new AlarmController();
        }
        return instance;
    }

    /**
     * Alarmı onaylar. (NFPA 72 §26.6.3.3.a)
     */
    public void acknowledgeAlarm(Alarm alarm, String note) {
        String operator = SessionManager.isLoggedIn() ? 
                SessionManager.getCurrentOperator().getName() + " (Sicil: " + SessionManager.getCurrentOperator().getEmployeeId() + ")" : "Bilinmeyen Operatör";
        String now = LocalDateTime.now().format(DISPLAY_FMT);

        updateStatusAndLog(alarm, "Onaylandı", note, operator, now);
        AlertSoundPlayer.getInstance().stop(); // Sesi sustur
        LOG.info("Alarm #" + alarm.getId() + " onaylandı. Operatör: " + operator);
    }

    /**
     * Alarmı reddeder / yanlış alarm olarak işaretler. (NFPA 72 §26.6.3.3.b)
     */
    public void rejectAlarm(Alarm alarm, String note) {
        String operator = SessionManager.isLoggedIn() ? 
                SessionManager.getCurrentOperator().getName() + " (Sicil: " + SessionManager.getCurrentOperator().getEmployeeId() + ")" : "Bilinmeyen Operatör";
        String now = LocalDateTime.now().format(DISPLAY_FMT);
        String finalNote = (note == null || note.trim().isEmpty()) ? "Yanlış alarm" : note;

        updateStatusAndLog(alarm, "Reddedildi", finalNote, operator, now);
        AlertSoundPlayer.getInstance().stop(); // Sesi sustur
        LOG.info("Alarm #" + alarm.getId() + " reddedildi. Operatör: " + operator);
    }

    /**
     * Ekibi çağırır ve bildirim gönderir. (NFPA 72 §26.6.3.5)
     */
    public void dispatchTeam(Alarm alarm) {
        AlertSoundPlayer.getInstance().playEvacuation();

        // Burada SMS/Email/MQTT bildirim mantığı çağrılabilir.
        // Basitlik adına, şu an logluyoruz ve DB'de notification_log'a yazıyoruz.
        String now = LocalDateTime.now().format(DISPLAY_FMT);
        LOG.info("Ekip çağırıldı — Alarm #" + alarm.getId());
        
        // Mock bildirim kaydı
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO notification_log (alarm_id, channel, recipient, sent_at, success, message_preview) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, alarm.getId());
            ps.setString(2, "SYSTEM");
            ps.setString(3, "Tüm Ekipler");
            ps.setString(4, now);
            ps.setInt(5, 1);
            ps.setString(6, "Acil durum tahliye tonu başlatıldı.");
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Bildirim logu yazılamadı: " + e.getMessage());
        }
    }

    private void updateStatusAndLog(Alarm alarm, String newStatus, String note, String operator, String timestamp) {
        try {
            Connection c = DatabaseManager.getConnection();

            // 1. alarms güncelle
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE alarms SET status=?, acknowledged_by=?, acknowledged_at=?, note=? WHERE id=?")) {
                ps.setString(1, newStatus);
                ps.setString(2, operator);
                ps.setString(3, timestamp);
                ps.setString(4, note);
                ps.setInt(5, alarm.getId());
                ps.executeUpdate();
            }

            // 2. alarm_state_log'a geçiş ekle
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO alarm_state_log (alarm_id, from_status, to_status, changed_by, changed_at, reason) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, alarm.getId());
                ps.setString(2, alarm.getStatus());
                ps.setString(3, newStatus);
                ps.setString(4, operator);
                ps.setString(5, timestamp);
                ps.setString(6, note);
                ps.executeUpdate();
            }

            // Objeyi de RAM'de güncelle
            // (gerçek bir model güncelleyicisi/setter metodları olmadığı için böyle kalabilir,
            // UI tekrar çekmek durumunda kalacak veya tablo kendisi güncellenecek).
        } catch (SQLException e) {
            LOG.severe("Alarm durumu güncellenemedi: " + e.getMessage());
        }
    }
}
