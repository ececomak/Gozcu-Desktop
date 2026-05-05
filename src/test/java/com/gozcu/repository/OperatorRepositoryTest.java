package com.gozcu.repository;

import com.gozcu.model.Operator;
import com.gozcu.util.DatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OperatorRepositoryTest {

    private static OperatorRepository repo;

    @BeforeAll
    public static void setup() {
        DatabaseManager.initializeDatabase();
        repo = new OperatorRepository();
    }

    @AfterAll
    public static void teardown() {
        DatabaseManager.closeConnection();
    }

    @Test
    public void testCrudOperations() {
        // 1. CREATE (Save)
        String uniqueId = "EMP" + System.currentTimeMillis();
        Operator newOp = new Operator("Test Yilmaz", uniqueId, "pass123", "Operator");
        
        boolean saved = repo.save(newOp);
        assertTrue(saved, "Operatör başarıyla kaydedilmeli.");
        
        // 2. READ (FindByEmployeeId)
        Operator found = repo.findByEmployeeId(uniqueId);
        assertNotNull(found, "Kaydedilen operatör bulunabilmeli.");
        assertEquals("Test Yilmaz", found.getName());
        assertEquals("Operator", found.getRole());
        
        // 3. READ ALL (FindAll)
        List<Operator> allOperators = repo.findAll();
        assertFalse(allOperators.isEmpty(), "Operatör listesi boş olmamalı.");
        
        boolean existsInList = allOperators.stream().anyMatch(o -> o.getEmployeeId().equals(uniqueId));
        assertTrue(existsInList, "Yeni eklenen operatör findAll listesinde bulunmalı.");
        
        // 4. DELETE
        boolean deleted = repo.delete(found.getId());
        assertTrue(deleted, "Operatör başarıyla silinmeli.");
        
        Operator afterDelete = repo.findByEmployeeId(uniqueId);
        assertNull(afterDelete, "Silinen operatör bulunamamalı.");
    }

    @Test
    public void testDuplicateEmployeeIdFails() {
        String duplicateId = "DUP123";
        Operator op1 = new Operator("Ahmet", duplicateId, "123", "Operator");
        Operator op2 = new Operator("Mehmet", duplicateId, "123", "Operator");
        
        assertTrue(repo.save(op1), "İlk operatör başarıyla kaydedilmeli.");
        assertFalse(repo.save(op2), "Aynı sicil no ile ikinci operatör kaydedilememeli (UNIQUE kısıtlaması).");
        
        // Temizlik
        Operator savedOp = repo.findByEmployeeId(duplicateId);
        if (savedOp != null) {
            repo.delete(savedOp.getId());
        }
    }
}
