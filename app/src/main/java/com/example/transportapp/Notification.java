package com.example.transportapp;

public class Notification {
    private String id;
    private String driverId;
    private String driverName;  // New field
    private String parentId;
    private String parentName;
    private String message;
    private long timestamp;
    private String type;  // "pickup" or "dropoff"

    public Notification() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }  // New getter
    public void setDriverName(String driverName) { this.driverName = driverName; }  // New setter

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}