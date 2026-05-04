package com.gozcu.util;

import javafx.scene.Scene;

/**
 * Uygulama genelinde CSS teması yönetimi.
 * AppSettings'den tema okunur, Scene'e uygulanır.
 */
public class ThemeManager {

    private static Scene scene;

    public static void register(Scene s) {
        scene = s;
        apply();
    }

    /** Mevcut ayara göre temayı uygular. */
    public static void apply() {
        if (scene == null) return;
        scene.getStylesheets().clear();
        try {
            String css = AppSettings.getTheme().equals("Açık Tema") ? "light.css" : "dark.css";
            var resource = ThemeManager.class.getResource("/styles/" + css);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
                System.out.println("Tema uygulandı: " + css);
            } else {
                System.err.println("Hata: Tema dosyası bulunamadı -> /styles/" + css);
            }
        } catch (Exception e) {
            System.err.println("Tema uygulanırken hata oluştu: " + e.getMessage());
        }
    }

    /** Temayı değiştir ve anında uygula. */
    public static void switchTheme(String themeName) {
        AppSettings.setTheme(themeName);
        AppSettings.save();
        apply();
    }
}
