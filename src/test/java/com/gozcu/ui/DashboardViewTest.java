package com.gozcu.ui;

import com.gozcu.model.Operator;
import com.gozcu.util.DatabaseManager;
import com.gozcu.util.SessionManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

public class DashboardViewTest extends ApplicationTest {

    @BeforeAll
    public static void setupSpec() {
        DatabaseManager.initializeDatabase();
        // Test için sahte giriş yap
        Operator testOp = new Operator(1, "Test Operator", "DASH_TEST", "123", "Operator", "now");
        SessionManager.login(testOp);
    }

    @AfterAll
    public static void teardownSpec() {
        SessionManager.logout();
        DatabaseManager.closeConnection();
    }

    @Override
    public void start(Stage stage) {
        DashboardView view = new DashboardView();
        Scene scene = new Scene(view.getView(), 1200, 800);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testDashboardComponentsLoaded() {
        // Arayüz yüklendiğinde, genel bileşenlerin görünür olduğunu kontrol et
        FxAssert.verifyThat("⬡  GÖZCÜ — Kontrol Merkezi", NodeMatchers.isVisible());
        FxAssert.verifyThat("DURUM PANELİ", NodeMatchers.isVisible());
        FxAssert.verifyThat("SON ALARMLAR", NodeMatchers.isVisible());
        
        // Stats labelleri ID ile kontrol et
        FxAssert.verifyThat("#activeCamLabel", NodeMatchers.isVisible());
        FxAssert.verifyThat("#todayAlarmLabel", NodeMatchers.isVisible());
        FxAssert.verifyThat("#criticalAlarmLabel", NodeMatchers.isVisible());
    }

    @Test
    public void testStatLabelsArePresent() {
        // İstatistik kartlarının etiketlerini kontrol et
        FxAssert.verifyThat("Aktif Kamera", NodeMatchers.isVisible());
        FxAssert.verifyThat("Bugünkü Alarm", NodeMatchers.isVisible());
        FxAssert.verifyThat("Kritik Alarm", NodeMatchers.isVisible());
    }
}
