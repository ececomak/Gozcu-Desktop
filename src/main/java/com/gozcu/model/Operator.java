package com.gozcu.model;

public class Operator {
    private int id;
    private String name;
    private String employeeId;
    private String passwordHash;
    private String role;
    private String createdAt;

    public Operator(int id, String name, String employeeId, String passwordHash, String role, String createdAt) {
        this.id = id;
        this.name = name;
        this.employeeId = employeeId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Operator(String name, String employeeId, String passwordHash, String role) {
        this.name = name;
        this.employeeId = employeeId;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmployeeId() { return employeeId; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
