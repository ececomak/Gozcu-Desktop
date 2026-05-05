package com.gozcu.ui;

import com.gozcu.model.Operator;
import com.gozcu.util.AppSettings;
import com.gozcu.util.DatabaseManager;
import com.gozcu.util.SessionManager;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsViewTest extends ApplicationTest {

    @BeforeAll
    public static void setupSpec() {
        DatabaseManager.initializeDatabase();
        // Test için Admin yetkisiyle sahte giriş yap
        Operator admin = new Operator(1, "Test Admin", "ADMIN_TEST", "123", "Admin", "now");
        SessionManager.login(admin);
    }

    @AfterAll
    public static void teardownSpec() {
        SessionManager.logout();
        DatabaseManager.closeConnection();
    }

    @Override
    public void start(Stage stage) {
        SettingsView view = new SettingsView();
        Scene scene = new Scene(view.getView(), 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testSystemTabComponents() {
        // Tab 1'in yüklendiğinden emin ol
        FxAssert.verifyThat("#thresholdSlider", NodeMatchers.isVisible());
        FxAssert.verifyThat("#themeBox", NodeMatchers.isVisible());
        FxAssert.verifyThat("#saveSystemBtn", NodeMatchers.isVisible());

        // Butona tıklandığında ayarları kaydettiğini uyarının çıktığı (alert) test edilebilir.
        // Ancak JavaFX Alert dialoglarını TestFX ile test etmek biraz zordur.
        // Şimdilik sadece bileşenlerin doğru şekilde yüklendiğini teyit ediyoruz.
    }

    @Test
    public void testNotificationTabComponents() {
        // Tab 2'ye (Ses ve Bildirimler) tıkla
        clickOn("🔊 Ses ve Bildirimler");

        // Checkbox'lar ve Buton gelmeli
        FxAssert.verifyThat("#soundCheck", NodeMatchers.isVisible());
        FxAssert.verifyThat("#notifCheck", NodeMatchers.isVisible());
        FxAssert.verifyThat("#saveNotifBtn", NodeMatchers.isVisible());
        
        // Etkileşim testi (checkbox işaretini kaldırıp geri koyma)
        clickOn("#soundCheck");
        clickOn("#notifCheck");
    }

    @Test
    public void testOperatorTabAdminAccess() {
        // Tab 3'e (Operatör Yönetimi) tıkla
        clickOn("👥 Operatör Yönetimi");

        // Admin olduğumuz için "Operatör Ekle" butonu görünür olmalı
        FxAssert.verifyThat("#btnAddOp", NodeMatchers.isVisible());
        FxAssert.verifyThat("#txtOpId", NodeMatchers.isVisible());
        FxAssert.verifyThat("#txtOpName", NodeMatchers.isVisible());
        
        // Yeni bir operatör eklemeyi test et
        clickOn("#txtOpId").write("TEST_EMP_" + System.currentTimeMillis());
        clickOn("#txtOpName").write("Veli Deneme");
        clickOn("#txtOpPass").write("password");
        
        // Ekle butonuna tıkla
        clickOn("#btnAddOp");
        
        // Alert çıkacağı için (Başarılı), dialog'da "Tamam" veya "OK" butonuna tıklamamız lazım.
        // Alert'ler dialog üstünde olduğu için "Tamam" veya İngilizceyse "OK" diyerek kapatabiliriz.
        try {
            clickOn("Tamam"); 
        } catch (Exception e) {
            try { clickOn("OK"); } catch (Exception ignored) {}
        }
    }
}
