package com.gozcu.util;

import com.gozcu.model.Operator;
import com.gozcu.repository.OperatorRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    @BeforeAll
    public static void setup() {
        DatabaseManager.initializeDatabase();
    }

    @AfterAll
    public static void teardown() {
        DatabaseManager.closeConnection();
    }

    @Test
    public void testDatabaseConnection() {
        try {
            Connection conn = DatabaseManager.getConnection();
            assertNotNull(conn, "Veritabanı bağlantısı null olmamalı.");
            assertFalse(conn.isClosed(), "Veritabanı bağlantısı açık olmalı.");
        } catch (Exception e) {
            fail("Bağlantı alınırken hata: " + e.getMessage());
        }
    }

    @Test
    public void testDefaultAdminExists() {
        OperatorRepository repo = new OperatorRepository();
        Operator admin = repo.findByEmployeeId("1234");
        
        assertNotNull(admin, "Varsayılan Admin (1234) veritabanında bulunmalı.");
        assertEquals("Sistem Yöneticisi", admin.getName());
        assertEquals("1234", admin.getPasswordHash());
        assertEquals("Admin", admin.getRole());
    }
}
