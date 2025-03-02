package com.example.transportapp;

public class Driver {
    private String id, name, phone, licenseNumber;

    public Driver() {}

    public Driver(String id, String name, String phone, String licenseNumber) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLicenseNumber() { return licenseNumber; }
}
