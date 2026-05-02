package com.gozcu.util;

public class AppSettings {

    private static boolean notificationsEnabled = true;
    private static boolean soundEnabled = true;
    private static int minimumConfidence = 60;
    private static String theme = "Açık Tema";

    public static boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public static void setNotificationsEnabled(boolean notificationsEnabled) {
        AppSettings.notificationsEnabled = notificationsEnabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean soundEnabled) {
        AppSettings.soundEnabled = soundEnabled;
    }

    public static int getMinimumConfidence() {
        return minimumConfidence;
    }

    public static void setMinimumConfidence(int minimumConfidence) {
        AppSettings.minimumConfidence = minimumConfidence;
    }

    public static String getTheme() {
        return theme;
    }

    public static void setTheme(String theme) {
        AppSettings.theme = theme;
    }
}