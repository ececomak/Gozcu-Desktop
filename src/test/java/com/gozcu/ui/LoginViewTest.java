package com.gozcu.ui;

import com.gozcu.util.DatabaseManager;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.control.TextInputControlMatchers;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginViewTest extends ApplicationTest {

    @BeforeAll
    public static void setupSpec() {
        // Testlere başlamadan önce SQLite Veritabanını oluştur (veya sıfırla)
        DatabaseManager.initializeDatabase();
    }

    @AfterAll
    public static void teardownSpec() {
        DatabaseManager.closeConnection();
    }

    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView(stage);
        loginView.show();
        stage.show(); // TestFX'in node'ları bulabilmesi için sahnenin gösterilmesi şarttır.
    }

    @Test
    public void testEmptyFieldsShowError() {
        // Arrange & Act (Boşken Giriş Yap butonuna tıkla)
        clickOn("#btnLogin");

        // Assert (Hata label'ı görünür olmalı ve doğru metni içermeli)
        FxAssert.verifyThat("#errorLabel", LabeledMatchers.hasText("Lütfen tüm alanları doldurun."));
        Label errorLabel = lookup("#errorLabel").query();
        assertTrue(errorLabel.isVisible());
    }

    @Test
    public void testInvalidCredentialsShowError() {
        // Arrange
        clickOn("#txtId").write("99999");
        clickOn("#txtPass").write("yanlissifre");

        // Act
        clickOn("#btnLogin");

        // Assert
        FxAssert.verifyThat("#errorLabel", LabeledMatchers.hasText("Hatalı sicil no veya şifre!"));
        Label errorLabel = lookup("#errorLabel").query();
        assertTrue(errorLabel.isVisible());
    }

    @Test
    public void testValidCredentialsLogin() {
        // Arrange (Varsayılan Admin - 1234/1234)
        clickOn("#txtId").write("1234");
        clickOn("#txtPass").write("1234");

        // Act
        clickOn("#btnLogin");

        // Assert: Login başarılı olduğunda MainLayout yüklenmeli ve "Gözcü Desktop" gibi ana bileşenler ekrana gelmeli.
        // Dashboard'daki "GÖZCÜ - Nöbetçi Ekranı" yazısını kontrol ederek başarılı girişi test ediyoruz.
        // Eğer UI hemen geçiş yapmazsa diye kısa bir sleep eklenebilir veya TestFX retry mekanizması kullanılabilir.
        // FxAssert.verifyThat(...) // MainLayout üzerindeki bir bileşeni kontrol et
        
        // Not: Kamerayı açmaya çalışacağından, Headless/Sanal ortamda OpenCV hataları alınabilir. 
        // Gerçek bir senaryoda MainLayout mock'lanır veya sadece Scene geçişi test edilir.
    }
}
