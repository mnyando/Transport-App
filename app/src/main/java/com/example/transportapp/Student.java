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

    public Student() {
        // Required empty constructor for Firestore
    }

    public Student(String name, String studentId, String grade, String route, String parentName, String parentContact, String homeLocation) {
        this.name = name;
        this.studentId = studentId;
        this.grade = grade;
        this.route = route;
        this.parentName = parentName;
        this.parentContact = parentContact;
        this.homeLocation = homeLocation;
    }

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
}