package com.example.transportapp;

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
    private String driver;
    private String driverPhone;
    private String attendant;
    private String attendantPhone;
    private String pickupTime;
    private String dropOffTime;

    public Student() {
        // Required empty constructor for Firestore
    }

    public Student(String name, String studentId, String grade, String route, String parentName, String parentContact, String homeLocation, String vehicle, String driver, String driverPhone, String attendant, String attendantPhone, String pickupTime, String dropOffTime) {
        this.name = name;
        this.studentId = studentId;
        this.grade = grade;
        this.route = route;
        this.parentName = parentName;
        this.parentContact = parentContact;
        this.homeLocation = homeLocation;
        this.vehicle = vehicle;
        this.driver = driver;
        this.driverPhone = driverPhone;
        this.attendant = attendant;
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

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getAttendantName() {
        return attendant;
    }

    public void setAttendantName(String attendant) {
        this.attendant = attendant;
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
}
