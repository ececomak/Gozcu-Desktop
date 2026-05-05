package com.gozcu.repository;

import com.gozcu.model.Operator;
import com.gozcu.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OperatorRepository {

    public Operator findByEmployeeId(String employeeId) {
        String sql = "SELECT * FROM operators WHERE employee_id = ?";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Operatör aranırken hata: " + e.getMessage());
        }
        return null;
    }

    public List<Operator> findAll() {
        List<Operator> list = new ArrayList<>();
        String sql = "SELECT * FROM operators ORDER BY created_at ASC";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Operatörler listelenirken hata: " + e.getMessage());
        }
        return list;
    }

    public boolean save(Operator op) {
        String sql = "INSERT INTO operators(name, employee_id, password_hash, role, created_at) " +
                     "VALUES(?, ?, ?, ?, datetime('now','localtime'))";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, op.getName());
            pstmt.setString(2, op.getEmployeeId());
            pstmt.setString(3, op.getPasswordHash());
            pstmt.setString(4, op.getRole());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Operatör eklenirken hata: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM operators WHERE id = ?";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Operatör silinirken hata: " + e.getMessage());
            return false;
        }
    }

    private Operator mapRow(ResultSet rs) throws SQLException {
        return new Operator(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("employee_id"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getString("created_at")
        );
    }
}
