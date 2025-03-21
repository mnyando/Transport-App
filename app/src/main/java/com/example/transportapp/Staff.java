package com.example.transportapp;


class Staff {
    private String id, staffName, phoneNumber, licenseNumber, assignedVehicle, assignedRoute, status, role;

    public Staff() {} // Empty constructor for Firestore

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
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
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
