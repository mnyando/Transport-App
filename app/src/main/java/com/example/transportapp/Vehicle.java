package com.example.transportapp;

public class Vehicle {
    private String id, vehicleName, vehicleNumber, capacity, status;

    // Default constructor for Firestore
    public Vehicle() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
