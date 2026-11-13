package com.example.transportapp;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

public class Vehicle {
    private String id, vehicleName, vehicleNumber, status;
    /**
     * Seats. Saved as a number since review #13; older records hold it as text, so this field
     * accepts either (Firestore hands over a Long or a String).
     */
    private Object capacity;
    /** Children assigned to this bus, kept up to date by StudentRecords and a Cloud Function. */
    @Nullable private Long studentCount;

    // Default constructor for Firestore
    public Vehicle() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public Object getCapacity() { return capacity; }
    public void setCapacity(Object capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    @Nullable public Long getStudentCount() { return studentCount; }
    public void setStudentCount(@Nullable Long studentCount) { this.studentCount = studentCount; }

    /** Capacity as a number, or -1 when missing or not a number. */
    @Exclude
    public int capacityValue() {
        return parseCapacity(capacity);
    }

    /** Capacity for display and edit boxes. */
    @Exclude
    public String capacityText() {
        int value = capacityValue();
        return value >= 0 ? String.valueOf(value) : (capacity == null ? "" : capacity.toString());
    }

    static int parseCapacity(@Nullable Object raw) {
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            try {
                return Integer.parseInt(((String) raw).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return -1;
    }
}
