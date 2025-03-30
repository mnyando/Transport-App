package com.example.transportapp;

import com.google.firebase.firestore.PropertyName;

public class Student {
    private String id;
    private String name;
    private String studentId;
    private String grade;
    private String route;
    private String parentName;
    private String parentContact;
    private String homeLocation;
    private String vehicle;
    private String driverName;  // Changed from 'driver' to match Firestore
    private String driverPhone;
    private String attendantName;  // Changed from 'attendant' to match Firestore
    private String attendantPhone;
    private String pickupTime;
    private String dropOffTime;

    // Required empty constructor for Firestore
    public Student() {}

    // Updated constructor to match new field names
    public Student(String name, String studentId, String grade, String route,
                   String parentName, String parentContact, String homeLocation,
                   String vehicle, String driverName, String driverPhone,
                   String attendantName, String attendantPhone,
                   String pickupTime, String dropOffTime) {
        this.name = name;
        this.studentId = studentId;
        this.grade = grade;
        this.route = route;
        this.parentName = parentName;
        this.parentContact = parentContact;
        this.homeLocation = homeLocation;
        this.vehicle = vehicle;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.attendantName = attendantName;
        this.attendantPhone = attendantPhone;
        this.pickupTime = pickupTime;
        this.dropOffTime = dropOffTime;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentContact() {
        return parentContact;
    }

    public void setParentContact(String parentContact) {
        this.parentContact = parentContact;
    }

    public String getHomeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(String homeLocation) {
        this.homeLocation = homeLocation;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    // Updated driver methods to match Firestore field
    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    // Updated attendant methods to match Firestore field
    public String getAttendantName() {
        return attendantName;
    }

    public void setAttendantName(String attendantName) {
        this.attendantName = attendantName;
    }

    public String getAttendantPhone() {
        return attendantPhone;
    }

    public void setAttendantPhone(String attendantPhone) {
        this.attendantPhone = attendantPhone;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getDropOffTime() {
        return dropOffTime;
    }

    public void setDropOffTime(String dropOffTime) {
        this.dropOffTime = dropOffTime;
    }

    // Optional: Add toString() for debugging
    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", grade='" + grade + '\'' +
                ", route='" + route + '\'' +
                ", driverName='" + driverName + '\'' +
                '}';
    }
}