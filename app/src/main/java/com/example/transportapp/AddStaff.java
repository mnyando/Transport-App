package com.example.transportapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
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

public class AddStaff extends AppCompatActivity {

    private EditText driverNameInput, driverPhoneInput, licenseNumberInput;
    private Spinner vehicleDropdown, routeDropdown;
    private RadioGroup statusRadioGroup, roleRadioGroup; // Added roleRadioGroup
    private RadioButton driverRadioButton, attendantRadioButton; // For role selection
    private Button saveDriverButton;
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
        driverNameInput = findViewById(R.id.driverNameInput);
        driverPhoneInput = findViewById(R.id.driverPhoneInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        roleRadioGroup = findViewById(R.id.roleRadioGroup); // Initialize new RadioGroup
        driverRadioButton = findViewById(R.id.driverRadioButton); // Initialize Driver button
        attendantRadioButton = findViewById(R.id.attendantRadioButton); // Initialize Attendant button
        saveDriverButton = findViewById(R.id.saveDriverButton);

        // Load data for spinners
        loadVehicles();
        loadRoutes();

        // Handle Save button click
        saveDriverButton.setOnClickListener(v -> saveStaffData()); // Renamed method for clarity
    }

    private void loadVehicles() {
        CollectionReference vehicleRef = db.collection("vehicle");
        vehicleRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
            vehicleList.add("Select Vehicle");
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
            routeList.add("Select Route");
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
        String staffName = driverNameInput.getText().toString().trim();
        String staffPhone = driverPhoneInput.getText().toString().trim();
        String licenseNumber = licenseNumberInput.getText().toString().trim();
        String assignedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String assignedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";
        int selectedStatusId = statusRadioGroup.getCheckedRadioButtonId();
        String status = selectedStatusId == R.id.activeRadioButton ? "Active" : "Not Active";
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        String role = selectedRoleId == R.id.driverRadioButton ? "Driver" : "Attendant";

        if (staffName.isEmpty() || staffPhone.isEmpty() || licenseNumber.isEmpty() ||
                "Select Vehicle".equals(assignedVehicle) || "Select Route".equals(assignedRoute)) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle and route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check staff count for the selected vehicle
        checkStaffCountForVehicle(assignedVehicle, () -> {
            Map<String, Object> staffData = new HashMap<>();
            staffData.put("staffName", staffName);
            staffData.put("phoneNumber", staffPhone);
            staffData.put("licenseNumber", licenseNumber);
            staffData.put("assignedVehicle", assignedVehicle);
            staffData.put("assignedRoute", assignedRoute);
            staffData.put("status", status);
            staffData.put("role", role);

            db.collection("staff").document(licenseNumber) // Use license as ID
                    .set(staffData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Staff added successfully", Toast.LENGTH_SHORT).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void checkStaffCountForVehicle(String vehicleName, Runnable onSuccess) {
        db.collection("staff") // Updated to "staff"
                .whereEqualTo("assignedVehicle", vehicleName)
                .whereEqualTo("role", "Driver") // Only count drivers
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int driverCount = queryDocumentSnapshots.size();
                    if (driverCount >= 2 && driverRadioButton.isChecked()) {
                        Toast.makeText(this, "Maximum of 2 drivers already assigned to " + vehicleName, Toast.LENGTH_SHORT).show();
                    } else {
                        onSuccess.run(); // Proceed with saving
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking staff count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        driverNameInput.setText("");
        driverPhoneInput.setText("");
        licenseNumberInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
        statusRadioGroup.check(R.id.activeRadioButton); // Reset to "Active"
        roleRadioGroup.check(R.id.driverRadioButton); // Reset to "Driver"
    }
}