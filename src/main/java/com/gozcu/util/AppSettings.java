package com.gozcu.util;

import java.io.*;
import java.util.Properties;

public class AppSettings {

    private static final String SETTINGS_FILE = "gozcu_settings.properties";

    private static boolean  notificationsEnabled = true;
    private static boolean  soundEnabled         = true;
    private static int      minimumConfidence    = 60;
    private static String   theme                = "Koyu Tema";
    private static String[] classNames           = {"fire", "smoke"};
    private static String   operatorName         = "Nöbetçi Operatör";

    // Bildirim hedefleri (notification.* anahtarları)
    private static final Properties ALL_PROPS = new Properties();

    // ── Yükle ────────────────────────────────────────────────────────────────

    public static void load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            ALL_PROPS.putAll(p);
            notificationsEnabled = Boolean.parseBoolean(p.getProperty("notifications", "true"));
            soundEnabled         = Boolean.parseBoolean(p.getProperty("sound", "true"));
            minimumConfidence    = Integer.parseInt(p.getProperty("minConfidence", "60"));
            theme                = p.getProperty("theme", "Koyu Tema");
            classNames           = p.getProperty("classNames", "fire,smoke").split(",");
            operatorName         = p.getProperty("operatorName", "Nöbetçi Operatör");
        } catch (IOException e) {
            System.err.println("Ayarlar yüklenemedi: " + e.getMessage());
        }
    }

    // ── Kaydet ───────────────────────────────────────────────────────────────

    public static void save() {
        Properties p = new Properties();
        p.setProperty("notifications", String.valueOf(notificationsEnabled));
        p.setProperty("sound",         String.valueOf(soundEnabled));
        p.setProperty("minConfidence", String.valueOf(minimumConfidence));
        p.setProperty("theme",         theme);
        p.setProperty("classNames",    String.join(",", classNames));
        p.setProperty("operatorName",  operatorName);
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            p.store(out, "Gozcu Settings");
        } catch (IOException e) {
            System.err.println("Ayarlar kaydedilemedi: " + e.getMessage());
        }
    }

    // ── Getter / Setter ───────────────────────────────────────────────────────

    public static boolean  isNotificationsEnabled()                 { return notificationsEnabled; }
    public static void     setNotificationsEnabled(boolean v)       { notificationsEnabled = v; }

    public static boolean  isSoundEnabled()                         { return soundEnabled; }
    public static void     setSoundEnabled(boolean v)               { soundEnabled = v; }

    public static int      getMinimumConfidence()                   { return minimumConfidence; }
    public static void     setMinimumConfidence(int v)              { minimumConfidence = v; }

    public static String   getTheme()                               { return theme; }
    public static void     setTheme(String v)                       { theme = v; }

    public static String[] getClassNames()                          { return classNames; }
    public static void     setClassNames(String[] v)                { classNames = v; }

    public static String   getOperatorName()                        { return operatorName; }
    public static void     setOperatorName(String v)                { operatorName = v; }

    /**
     * Genel anahtar-değer getter — bildirim hedefleri gibi dinamik ayarlar için.
     *
     * @param key          Ayar anahtarı (örn: "notification.sms")
     * @param defaultValue Anahtar yoksa döndürülecek varsayılan
     * @return Ayar değeri veya varsayılan
     */
    public static String get(String key, String defaultValue) {
        return ALL_PROPS.getProperty(key, defaultValue);
    }

    /** Dinamik anahtar-değer ayarla. */
    public static void set(String key, String value) {
        ALL_PROPS.setProperty(key, value);
    }
}