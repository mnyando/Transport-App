package com.example.transportapp;

public class Driver {
    private String id;
    private String staffName;
    private String staffPhone;
    private String staffLicense;
    private String assignedVehicle;
    private String assignedRoute;

    // Required empty constructor for Firestore
    public Driver() {}

    public Driver(String id, String staffName, String staffPhone, String staffLicense, String assignedVehicle, String assignedRoute) {
        this.id = id;
        this.staffName = staffName;
        this.staffPhone = staffPhone;
        this.staffLicense = staffLicense;
        this.assignedVehicle = assignedVehicle;
        this.assignedRoute = assignedRoute;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffPhone() {
        return staffPhone;
    }

    public void setStaffPhone(String staffPhone) {
        this.staffPhone = staffPhone;
    }

    public String getStaffLicense() {
        return staffLicense;
    }

    public void setStaffLicense(String staffLicense) {
        this.staffLicense = staffLicense;
    }

    public String getAssignedVehicle() {
        return assignedVehicle;
    }

    public void setAssignedVehicle(String assignedVehicle) {
        this.assignedVehicle = assignedVehicle;
    }

    public String getAssignedRoute() {
        return assignedRoute;
    }

    public void setAssignedRoute(String assignedRoute) {
        this.assignedRoute = assignedRoute;
    }
}