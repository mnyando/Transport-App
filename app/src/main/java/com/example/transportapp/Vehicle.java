package com.example.transportapp;

public class Vehicle {
    private String id;
    private String vehicleName;
    private String vehicleNumber;
    private String capacity;

    // Empty constructor for Firestore
    public Vehicle() {}

    public Vehicle(String id, String vehicleName, String vehicleNumber, String capacity) {
        this.id = id;
        this.vehicleName = vehicleName;
        this.vehicleNumber = vehicleNumber;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getCapacity() {
        return capacity;
    }
}
