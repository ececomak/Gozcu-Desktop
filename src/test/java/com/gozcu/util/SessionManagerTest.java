package com.gozcu.util;

import com.gozcu.model.Operator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    @BeforeEach
    public void setup() {
        SessionManager.logout();
    }

    @AfterEach
    public void cleanup() {
        // Her testten sonra oturumu temizle
        SessionManager.logout();
    }

    @Test
    public void testLoginAndLogout() {
        Operator op = new Operator(1, "Test Operator", "999", "pass", "Operator", "now");
        
        assertFalse(SessionManager.isLoggedIn(), "Başlangıçta oturum kapalı olmalı");
        
        SessionManager.login(op);
        assertTrue(SessionManager.isLoggedIn(), "Giriş yapıldıktan sonra oturum açık olmalı");
        assertEquals("Test Operator", SessionManager.getCurrentOperator().getName());
        
        SessionManager.logout();
        assertFalse(SessionManager.isLoggedIn(), "Çıkış yapıldıktan sonra oturum kapalı olmalı");
        assertNull(SessionManager.getCurrentOperator());
    }

    @Test
    public void testIsAdminRole() {
        Operator adminOp = new Operator(2, "Admin User", "111", "pass", "Admin", "now");
        Operator standardOp = new Operator(3, "Standard User", "222", "pass", "Operator", "now");
        
        // Admin Girişi
        SessionManager.login(adminOp);
        assertTrue(SessionManager.isAdmin(), "Admin rolündeki kullanıcı için isAdmin true dönmeli");
        
        // Standart Operatör Girişi
        SessionManager.login(standardOp);
        assertFalse(SessionManager.isAdmin(), "Operator rolündeki kullanıcı için isAdmin false dönmeli");
        
        // Çıkış
        SessionManager.logout();
        assertFalse(SessionManager.isAdmin(), "Oturum kapalıyken isAdmin false dönmeli");
    }
}
