package com.example.transportapp;

public class Student {
    private String id, name, studentId, grade, parentName, parentContact, homeLocation;

    public Student() {} // Firestore needs empty constructor

    public Student(String name, String studentId, String grade, String parentName, String parentContact, String homeLocation) {
        this.name = name;
        this.studentId = studentId;
        this.grade = grade;
        this.parentName = parentName;
        this.parentContact = parentContact;
        this.homeLocation = homeLocation;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getStudentId() { return studentId; }
    public String getGrade() { return grade; }
    public String getParentName() { return parentName; }
    public String getParentContact() { return parentContact; }
    public String getHomeLocation() { return homeLocation; }
}
