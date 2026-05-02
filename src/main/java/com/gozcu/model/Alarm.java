package com.gozcu.model;

public class Alarm {

    private int id;
    private String cameraName;
    private String location;
    private String alarmType;
    private String level;
    private double confidence;
    private String status;
    private String detectionTime;
    private String note;

    public Alarm(int id, String cameraName, String location, String alarmType, String level,
                 double confidence, String status, String detectionTime, String note) {
        this.id = id;
        this.cameraName = cameraName;
        this.location = location;
        this.alarmType = alarmType;
        this.level = level;
        this.confidence = confidence;
        this.status = status;
        this.detectionTime = detectionTime;
        this.note = note;
    }

    public Alarm(String cameraName, String location, String alarmType, String level,
                 double confidence, String status, String detectionTime, String note) {
        this.cameraName = cameraName;
        this.location = location;
        this.alarmType = alarmType;
        this.level = level;
        this.confidence = confidence;
        this.status = status;
        this.detectionTime = detectionTime;
        this.note = note;
    }

    public int getId() { return id; }
    public String getCameraName() { return cameraName; }
    public String getLocation() { return location; }
    public String getAlarmType() { return alarmType; }
    public String getLevel() { return level; }
    public double getConfidence() { return confidence; }
    public String getStatus() { return status; }
    public String getDetectionTime() { return detectionTime; }
    public String getNote() { return note; }
}