package com.example.transportapp;

class Driver {
    private String id, driverName, phoneNumber, licenseNumber, assignedVehicle, assignedRoute, status;

    public Driver() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getAssignedVehicle() { return assignedVehicle; }
    public void setAssignedVehicle(String assignedVehicle) { this.assignedVehicle = assignedVehicle; }
    public String getAssignedRoute() { return assignedRoute; }
    public void setAssignedRoute(String assignedRoute) { this.assignedRoute = assignedRoute; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}