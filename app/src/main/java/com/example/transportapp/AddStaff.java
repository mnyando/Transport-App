package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStaff extends AppCompatActivity {

    private EditText staffNameInput, staffPhoneInput, staffLicenseInput;
    private Spinner vehicleDropdown, routeDropdown;
    private Button saveStaffButton;

    private FirebaseFirestore db;
    private List<String> vehicleList = new ArrayList<>();
    private List<String> routeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        staffNameInput = findViewById(R.id.driverNameInput);
        staffPhoneInput = findViewById(R.id.driverPhoneInput);
        staffLicenseInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        saveStaffButton = findViewById(R.id.saveDriverButton);

        // Load data for spinners
        loadVehicles();
        loadRoutes();

        // Handle Save button click
        saveStaffButton.setOnClickListener(v -> saveStaffData());
    }

    private void loadVehicles() {
        CollectionReference vehicleRef = db.collection("vehicle");
        vehicleRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
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
        CollectionReference routeRef = db.collection("Routes"); // Fixed collection name to "Routes"
        routeRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            routeList.clear();
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

    private void saveStaffData() {
        String staffName = staffNameInput.getText().toString().trim();
        String staffPhone = staffPhoneInput.getText().toString().trim();
        String staffLicense = staffLicenseInput.getText().toString().trim();
        String assignedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String assignedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";

        if (staffName.isEmpty() || staffPhone.isEmpty() || staffLicense.isEmpty() || assignedVehicle.isEmpty() || assignedRoute.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle and route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check staff count for the selected vehicle
        checkStaffCountForVehicle(assignedVehicle, () -> {
            // Proceed with saving if staff count is less than 2
            Map<String, Object> staffData = new HashMap<>();
            staffData.put("staffName", staffName);
            staffData.put("staffPhone", staffPhone);
            staffData.put("staffLicense", staffLicense);
            staffData.put("assignedVehicle", assignedVehicle);
            staffData.put("assignedRoute", assignedRoute);

            db.collection("staff").add(staffData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Staff added successfully", Toast.LENGTH_SHORT).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void checkStaffCountForVehicle(String vehicleName, Runnable onSuccess) {
        db.collection("staff")
                .whereEqualTo("assignedVehicle", vehicleName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int staffCount = queryDocumentSnapshots.size();
                    if (staffCount >= 2) {
                        Toast.makeText(this, "Maximum of 2 staff already assigned to " + vehicleName, Toast.LENGTH_SHORT).show();
                    } else {
                        onSuccess.run(); // Proceed with saving
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking staff count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        staffNameInput.setText("");
        staffPhoneInput.setText("");
        staffLicenseInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
    }
}