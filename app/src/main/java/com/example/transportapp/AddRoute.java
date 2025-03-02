package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRoute extends AppCompatActivity {

    private EditText routeNameEditText;
    private EditText totalDistanceInput;
    private EditText routeDescriptionInput;
    private Spinner vehicleDropdown;
    private Button addRouteButton;

    private FirebaseFirestore firestore;
    private List<String> vehicleNamesList;
    private ArrayAdapter<String> vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        // Initialize UI elements
        routeNameEditText = findViewById(R.id.routeNameEditText);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        addRouteButton = findViewById(R.id.addRouteButton);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize vehicle list and adapter
        vehicleNamesList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleNamesList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);

        // Fetch vehicle names
        fetchVehicleNames();

        // Set button click listener
        addRouteButton.setOnClickListener(v -> addRouteToFirestore());
    }

    private void fetchVehicleNames() {
        firestore.collection("vehicle")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleNamesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String vehicleName = document.getString("vehicleName"); // Adjust field name as needed
                        if (vehicleName != null) {
                            vehicleNamesList.add(vehicleName);
                        }
                    }
                    vehicleAdapter.notifyDataSetChanged();
                    if (vehicleNamesList.isEmpty()) {
                        Toast.makeText(this, "No vehicles found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addRouteToFirestore() {
        String routeName = routeNameEditText.getText().toString().trim();
        String totalDistanceStr = totalDistanceInput.getText().toString().trim();
        String routeDescription = routeDescriptionInput.getText().toString().trim();
        String selectedVehicle = vehicleDropdown.getSelectedItem() != null ?
                vehicleDropdown.getSelectedItem().toString() : "";

        if (routeName.isEmpty() || totalDistanceStr.isEmpty() || routeDescription.isEmpty() || selectedVehicle.isEmpty()) {
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

        Map<String, Object> routeData = new HashMap<>();
        routeData.put("routeName", routeName);
        routeData.put("totalDistance", totalDistance);
        routeData.put("routeDescription", routeDescription);
        routeData.put("vehicle", selectedVehicle);

        firestore.collection("Routes")
                .add(routeData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Route added successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        routeNameEditText.setText("");
        totalDistanceInput.setText("");
        routeDescriptionInput.setText("");
        vehicleDropdown.setSelection(0);
    }
}