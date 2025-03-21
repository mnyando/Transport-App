package com.example.transportapp;

// Route model class (assumed, updated to include pickupTime and dropOffTime)
class Route {
    private String id, routeName, routeDescription, vehicle, pickupTime, dropOffTime;
    private double totalDistance;

    public Route() {} // Empty constructor for Firestore

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    public String getRouteDescription() { return routeDescription; }
    public void setRouteDescription(String routeDescription) { this.routeDescription = routeDescription; }
    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public String getDropOffTime() { return dropOffTime; }
    public void setDropOffTime(String dropOffTime) { this.dropOffTime = dropOffTime; }
}
