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
    private String driverName;
    private String driverPhone;
    private String attendantName;
    private String attendantPhone;
    private String pickupTime;
    private String dropOffTime;
    private PickupStatus pickupStatus;

    // Required empty constructor for Firestore
    public Student() {}

    // Existing constructor
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

    // PickupStatus inner class
    public static class PickupStatus {
        private boolean isPicked;
        private long timestamp;
        private String tripId;

        public PickupStatus() {}

        public PickupStatus(boolean isPicked, long timestamp, String tripId) {
            this.isPicked = isPicked;
            this.timestamp = timestamp;
            this.tripId = tripId;
        }

        @PropertyName("isPicked")
        public boolean isPicked() { return isPicked; }

        @PropertyName("isPicked")
        public void setPicked(boolean picked) { isPicked = picked; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getTripId() { return tripId; }
        public void setTripId(String tripId) { this.tripId = tripId; }
    }

    // Getters and Setters (existing ones remain unchanged)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentContact() { return parentContact; }
    public void setParentContact(String parentContact) { this.parentContact = parentContact; }
    public String getHomeLocation() { return homeLocation; }
    public void setHomeLocation(String homeLocation) { this.homeLocation = homeLocation; }
    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getAttendantName() { return attendantName; }
    public void setAttendantName(String attendantName) { this.attendantName = attendantName; }
    public String getAttendantPhone() { return attendantPhone; }
    public void setAttendantPhone(String attendantPhone) { this.attendantPhone = attendantPhone; }
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public String getDropOffTime() { return dropOffTime; }
    public void setDropOffTime(String dropOffTime) { this.dropOffTime = dropOffTime; }

    // New pickup status methods
    public PickupStatus getPickupStatus() { return pickupStatus; }
    public void setPickupStatus(PickupStatus pickupStatus) { this.pickupStatus = pickupStatus; }

    public void updatePickupStatus(boolean isPicked, String tripId) {
        if (this.pickupStatus == null) {
            this.pickupStatus = new PickupStatus(isPicked, System.currentTimeMillis(), tripId);
        } else {
            this.pickupStatus.setPicked(isPicked);
            this.pickupStatus.setTimestamp(System.currentTimeMillis());
            this.pickupStatus.setTripId(tripId);
        }
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", vehicle='" + vehicle + '\'' +
                ", pickupStatus=" + (pickupStatus != null ? pickupStatus.isPicked() : "null") +
                '}';
    }
}