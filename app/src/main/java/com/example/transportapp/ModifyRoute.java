package com.example.transportapp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyRoute extends AppCompatActivity {

    private EditText routeNameInput, totalDistanceInput, routeDescriptionInput;
    private EditText pickupTimeInput, dropOffTimeInput; // New fields
    private Spinner vehicleDropdown;
    private Button updateRouteButton;
    private RecyclerView routeRecyclerView;

    private FirebaseFirestore firestore;
    private List<Route> routeList;
    private RouteAdapter routeAdapter;
    private String selectedRouteId;
    private List<String> vehicleNamesList;
    private ArrayAdapter<String> vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_route);

        // Initialize UI elements
        routeNameInput = findViewById(R.id.routeNameInput);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        pickupTimeInput = findViewById(R.id.pickupTimeInput); // Initialize pickup time
        dropOffTimeInput = findViewById(R.id.dropOffTimeInput); // Initialize drop-off time
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        updateRouteButton = findViewById(R.id.updateRouteButton);
        routeRecyclerView = findViewById(R.id.routeRecyclerView);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        routeList = new ArrayList<>();
        routeAdapter = new RouteAdapter(routeList, this::onRouteSelected);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeRecyclerView.setAdapter(routeAdapter);

        // Initialize Spinner with vehicle names
        vehicleNamesList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleNamesList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);

        // Fetch data
        fetchVehicles();
        fetchRoutes();

        // Set button click listener for updating route
        updateRouteButton.setOnClickListener(v -> updateRouteInFirestore());

        // Set click listeners for time inputs to show TimePickerDialog
        pickupTimeInput.setOnClickListener(v -> showTimePickerDialog(pickupTimeInput));
        dropOffTimeInput.setOnClickListener(v -> showTimePickerDialog(dropOffTimeInput));
    }

    private void fetchVehicles() {
        firestore.collection("vehicle")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleNamesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String vehicleName = document.getString("vehicleName");
                        if (vehicleName != null) {
                            vehicleNamesList.add(vehicleName);
                        }
                    }
                    vehicleAdapter.notifyDataSetChanged();
                    if (vehicleNamesList.isEmpty()) {
                        Toast.makeText(this, "No vehicles found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchRoutes() {
        firestore.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Route route = document.toObject(Route.class);
                        route.setId(document.getId());
                        routeList.add(route);
                    }
                    routeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch routes", Toast.LENGTH_SHORT).show());
    }

    private void onRouteSelected(Route route) {
        selectedRouteId = route.getId();
        routeNameInput.setText(route.getRouteName());
        totalDistanceInput.setText(String.valueOf(route.getTotalDistance()));
        routeDescriptionInput.setText(route.getRouteDescription());
        pickupTimeInput.setText(route.getPickupTime()); // Set pickup time from database
        dropOffTimeInput.setText(route.getDropOffTime()); // Set drop-off time from database

        // Set the Spinner to the route's vehicle
        int position = vehicleAdapter.getPosition(route.getVehicle());
        if (position >= 0) {
            vehicleDropdown.setSelection(position);
        } else {
            vehicleDropdown.setSelection(0); // Default to first item if not found
        }
    }

    private void updateRouteInFirestore() {
        if (selectedRouteId == null) {
            Toast.makeText(this, "Please select a route to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String routeName = routeNameInput.getText().toString().trim();
        String totalDistanceStr = totalDistanceInput.getText().toString().trim();
        String routeDescription = routeDescriptionInput.getText().toString().trim();
        String selectedVehicle = vehicleDropdown.getSelectedItem() != null ?
                vehicleDropdown.getSelectedItem().toString() : "";
        String pickupTime = pickupTimeInput.getText().toString().trim(); // Get pickup time
        String dropOffTime = dropOffTimeInput.getText().toString().trim(); // Get drop-off time

        if (routeName.isEmpty() || totalDistanceStr.isEmpty() || routeDescription.isEmpty() ||
                selectedVehicle.isEmpty() || pickupTime.isEmpty() || dropOffTime.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalDistance;
        try {
            totalDistance = Double.parseDouble(totalDistanceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare updated data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("routeName", routeName);
        updatedData.put("totalDistance", totalDistance);
        updatedData.put("routeDescription", routeDescription);
        updatedData.put("vehicle", selectedVehicle);
        updatedData.put("pickupTime", pickupTime); // Include pickup time
        updatedData.put("dropOffTime", dropOffTime); // Include drop-off time

        // Update Firestore
        firestore.collection("Routes")
                .document(selectedRouteId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Route updated successfully", Toast.LENGTH_SHORT).show();
                    fetchRoutes(); // Refresh the route list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update route: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showTimePickerDialog(EditText targetEditText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute); // Ensures "05:40"
                    targetEditText.setText(time);
                }, 12, 0, true); // 24-hour format, default to 12:00
        timePickerDialog.show();
    }
}

