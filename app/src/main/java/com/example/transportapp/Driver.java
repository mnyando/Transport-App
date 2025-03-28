package com.example.transportapp;

public class Driver {
    private String id;
    private String staffName;  // Updated from driverName to match Firestore field
    private String phoneNumber;
    private String licenseNumber;
    private String assignedVehicle;
    private String assignedRoute;
    private String status;
    private String role;  // Added role field (Driver or Attendant)

    // Default constructor required for Firestore
    public Driver() {}

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
