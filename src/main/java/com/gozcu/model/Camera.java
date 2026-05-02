package com.gozcu.model;

public class Camera {

    private int id;
    private String name;
    private String location;
    private String source;
    private String status;
    private int sensitivity;

    public Camera(int id, String name, String location, String source, String status, int sensitivity) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.source = source;
        this.status = status;
        this.sensitivity = sensitivity;
    }

    public Camera(String name, String location, String source, String status, int sensitivity) {
        this.name = name;
        this.location = location;
        this.source = source;
        this.status = status;
        this.sensitivity = sensitivity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public int getSensitivity() { return sensitivity; }

    @Override
    public String toString() {
        return name + " - " + location;
    }
}