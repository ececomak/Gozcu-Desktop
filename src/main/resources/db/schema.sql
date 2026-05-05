-- ============================================================
-- GÖZCÜ Desktop — Veritabanı Şeması
-- Standart: NFPA 72 (2022) §26.6 Alarm Kayıt Gereksinimleri
--            IEC 62682 Alarm Yönetim Sistemi
-- Uyumluluk: SQLite 3.x / H2 / PostgreSQL
-- Oluşturma tarihi: 2026-05
-- ============================================================

-- ------------------------------------------------------------
-- 1. ALARMLAR — Her tespit edilen yangın/duman olayı
-- NFPA 72 §26.6.3: Alarm kaydı zorunlu alanlar
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alarms (
    id              INTEGER      PRIMARY KEY AUTOINCREMENT,
    camera_name     TEXT         NOT NULL,               -- Kaynak kamera etiketi
    location        TEXT         NOT NULL,               -- Fiziksel konum tanımı
    alarm_type      TEXT         NOT NULL,               -- 'fire' | 'smoke' | 'both'
    level           TEXT         NOT NULL                -- NFPA 72 §26.6.3.1 seviyesi
                    CHECK (level IN ('Kritik','Orta','Düşük')),
    confidence      REAL         NOT NULL                -- Model güven skoru [0.0–1.0]
                    CHECK (confidence BETWEEN 0.0 AND 1.0),
    status          TEXT         NOT NULL DEFAULT 'Yeni' -- İş akışı durumu
                    CHECK (status IN ('Yeni','Onaylandı','Reddedildi','Kapatıldı')),
    detection_time  TEXT         NOT NULL,               -- ISO-8601: YYYY-MM-DDTHH:mm:ss
    acknowledged_by TEXT,                                -- Onaylayan operatör adı
    acknowledged_at TEXT,                                -- Onay zamanı ISO-8601
    note            TEXT,                                -- Serbest metin notu
    cap_message_id  TEXT                                 -- CAP v1.2 identifier (UUID)
);

-- Zaman bazlı sorguları hızlandırır (raporlama, günlük/haftalık istatistik)
CREATE INDEX IF NOT EXISTS idx_alarms_detection_time ON alarms (detection_time);
-- Durum filtresi için
CREATE INDEX IF NOT EXISTS idx_alarms_status         ON alarms (status);

-- ------------------------------------------------------------
-- 2. ALARM DURUM LOGu — Her durum geçişini kayıt altına alır
-- IEC 62682 §8.4: Alarm lifecycle traceability
-- NFPA 72 §26.6.3.4: Onay ve kapatma zamanları raporlanabilir olmalı
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alarm_state_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    alarm_id    INTEGER NOT NULL
                REFERENCES alarms(id) ON DELETE CASCADE,
    from_status TEXT    NOT NULL,        -- Önceki durum
    to_status   TEXT    NOT NULL,        -- Yeni durum
    changed_by  TEXT    NOT NULL,        -- Kullanıcı adı / 'SYSTEM'
    changed_at  TEXT    NOT NULL,        -- ISO-8601 zaman damgası
    reason      TEXT                     -- Açıklama (isteğe bağlı)
);

CREATE INDEX IF NOT EXISTS idx_asl_alarm_id  ON alarm_state_log (alarm_id);
CREATE INDEX IF NOT EXISTS idx_asl_changed_at ON alarm_state_log (changed_at);

-- ------------------------------------------------------------
-- 3. BİLDİRİM LOGU — SMS / E-posta / MQTT gönderim kayıtları
-- NFPA 72 §26.6.3.5: Bildirim geçmişi 1 yıl saklanmalıdır
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    alarm_id        INTEGER NOT NULL
                    REFERENCES alarms(id) ON DELETE SET NULL,
    channel         TEXT    NOT NULL              -- 'SMS' | 'EMAIL' | 'MQTT'
                    CHECK (channel IN ('SMS','EMAIL','MQTT')),
    recipient       TEXT    NOT NULL,             -- Telefon no / e-posta / MQTT topic
    message_preview TEXT,                         -- İlk 200 karakter
    sent_at         TEXT    NOT NULL,             -- ISO-8601
    success         INTEGER NOT NULL DEFAULT 1    -- 1=Başarılı, 0=Başarısız (BOOLEAN)
                    CHECK (success IN (0, 1)),
    error_detail    TEXT                          -- Hata varsa açıklama
);

CREATE INDEX IF NOT EXISTS idx_nl_alarm_id ON notification_log (alarm_id);
CREATE INDEX IF NOT EXISTS idx_nl_sent_at  ON notification_log (sent_at);
CREATE INDEX IF NOT EXISTS idx_nl_channel  ON notification_log (channel);

-- ------------------------------------------------------------
-- 4. KAMERALAR — (Mevcut tablo, referans için)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cameras (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    location    TEXT    NOT NULL,
    source      TEXT    NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'Aktif',
    sensitivity INTEGER NOT NULL DEFAULT 60
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cameras_source ON cameras (source);
