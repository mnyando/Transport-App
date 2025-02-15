package com.example.transportapp;

public class Route {
    private String id;
    private String routeName;
    private double totalDistance;
    private String routeDescription;
    private String vehicle;

    public Route() {
        // Empty constructor needed for Firestore
    }

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
}

