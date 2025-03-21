package com.example.transportapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStaff extends AppCompatActivity { // Changed to AddDriver to match XML intent

    private EditText driverNameInput, driverPhoneInput, licenseNumberInput; // Renamed for clarity
    private Spinner vehicleDropdown, routeDropdown;
    private RadioGroup statusRadioGroup; // Added RadioGroup
    private Button saveDriverButton; // Renamed for clarity
    private FirebaseFirestore db;
    private List<String> vehicleList = new ArrayList<>();
    private List<String> routeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff); // Updated to match provided XML

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        driverNameInput = findViewById(R.id.driverNameInput);
        driverPhoneInput = findViewById(R.id.driverPhoneInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        statusRadioGroup = findViewById(R.id.statusRadioGroup); // Initialize RadioGroup
        saveDriverButton = findViewById(R.id.saveDriverButton);

        // Load data for spinners
        loadVehicles();
        loadRoutes();

        // Handle Save button click
        saveDriverButton.setOnClickListener(v -> saveDriverData());
    }

    private void loadVehicles() {
        CollectionReference vehicleRef = db.collection("vehicle");
        vehicleRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
            vehicleList.add("Select Vehicle"); // Add default option
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String vehicleName = doc.getString("vehicleName");
                if (vehicleName != null) {
                    vehicleList.add(vehicleName);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vehicleDropdown.setAdapter(adapter);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadRoutes() {
        CollectionReference routeRef = db.collection("Routes");
        routeRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            routeList.clear();
            routeList.add("Select Route"); // Add default option
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String routeName = doc.getString("routeName");
                if (routeName != null) {
                    routeList.add(routeName);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeDropdown.setAdapter(adapter);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveDriverData() {
        String driverName = driverNameInput.getText().toString().trim();
        String driverPhone = driverPhoneInput.getText().toString().trim();
        String licenseNumber = licenseNumberInput.getText().toString().trim();
        String assignedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String assignedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";
        int selectedStatusId = statusRadioGroup.getCheckedRadioButtonId();
        String status = selectedStatusId == R.id.activeRadioButton ? "Active" : "Not Active";

        if (driverName.isEmpty() || driverPhone.isEmpty() || licenseNumber.isEmpty() ||
                "Select Vehicle".equals(assignedVehicle) || "Select Route".equals(assignedRoute)) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle and route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check staff count for the selected vehicle
        checkStaffCountForVehicle(assignedVehicle, () -> {
            // Proceed with saving if staff count is less than 2
            Map<String, Object> driverData = new HashMap<>();
            driverData.put("driverName", driverName); // Updated field names for consistency
            driverData.put("phoneNumber", driverPhone);
            driverData.put("licenseNumber", licenseNumber);
            driverData.put("assignedVehicle", assignedVehicle);
            driverData.put("assignedRoute", assignedRoute);
            driverData.put("status", status); // Add status to driver data

            db.collection("drivers") // Updated to "drivers" to match XML intent
                    .add(driverData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Driver added successfully", Toast.LENGTH_SHORT).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving driver: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void checkStaffCountForVehicle(String vehicleName, Runnable onSuccess) {
        db.collection("drivers") // Updated to "drivers"
                .whereEqualTo("assignedVehicle", vehicleName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int staffCount = queryDocumentSnapshots.size();
                    if (staffCount >= 2) {
                        Toast.makeText(this, "Maximum of 2 drivers already assigned to " + vehicleName, Toast.LENGTH_SHORT).show();
                    } else {
                        onSuccess.run(); // Proceed with saving
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking driver count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        driverNameInput.setText("");
        driverPhoneInput.setText("");
        licenseNumberInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
        statusRadioGroup.check(R.id.activeRadioButton); // Reset to default "Active"
    }
}