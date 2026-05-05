package com.gozcu.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class AppSettingsTest {

    private static final String TEST_SETTINGS_FILE = "gozcu_settings.properties";

    @BeforeEach
    public void setup() {
        // Test öncesi varsayılan değerlere döndür
        AppSettings.setNotificationsEnabled(true);
        AppSettings.setSoundEnabled(true);
        AppSettings.setMinimumConfidence(60);
        AppSettings.setTheme("Koyu Tema");
    }

    @AfterEach
    public void cleanup() {
        // Testler bittikten sonra test dosyasını sil
        File file = new File(TEST_SETTINGS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testSaveAndLoadSettings() {
        // 1. Değerleri Değiştir
        AppSettings.setNotificationsEnabled(false);
        AppSettings.setSoundEnabled(false);
        AppSettings.setMinimumConfidence(85);
        AppSettings.setTheme("Açık Tema");
        AppSettings.set("custom.test.key", "customValue");
        
        // 2. Kaydet
        AppSettings.save();
        
        File file = new File(TEST_SETTINGS_FILE);
        assertTrue(file.exists(), "Ayarlar dosyası oluşturulmalı.");
        
        // 3. Hafızadaki değerleri boz
        AppSettings.setNotificationsEnabled(true);
        AppSettings.setSoundEnabled(true);
        AppSettings.setMinimumConfidence(10);
        AppSettings.setTheme("Bozuk Tema");
        
        // 4. Dosyadan Geri Yükle
        AppSettings.load();
        
        // 5. Doğrula
        assertFalse(AppSettings.isNotificationsEnabled(), "Bildirim ayarı dosyadan doğru okunmalı.");
        assertFalse(AppSettings.isSoundEnabled(), "Ses ayarı dosyadan doğru okunmalı.");
        assertEquals(85, AppSettings.getMinimumConfidence(), "Güven eşiği dosyadan doğru okunmalı.");
        assertEquals("Açık Tema", AppSettings.getTheme(), "Tema dosyadan doğru okunmalı.");
        assertEquals("customValue", AppSettings.get("custom.test.key", ""), "Dinamik değerler doğru okunmalı.");
    }
}
