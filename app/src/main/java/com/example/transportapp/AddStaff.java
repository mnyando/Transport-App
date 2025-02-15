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
        saveStaffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStaffData();
            }
        });
    }

    private void loadVehicles() {
        CollectionReference vehicleRef = db.collection("vehicle");
        vehicleRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String vehicleName = doc.getString("vehicleName");
                vehicleList.add(vehicleName);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vehicleDropdown.setAdapter(adapter);
        });
    }

    private void loadRoutes() {
        CollectionReference routeRef = db.collection("route");
        routeRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            routeList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String routeName = doc.getString("routeName");
                routeList.add(routeName);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeDropdown.setAdapter(adapter);
        });
    }

    private void saveStaffData() {
        String staffName = staffNameInput.getText().toString().trim();
        String staffPhone = staffPhoneInput.getText().toString().trim();
        String staffLicense = staffLicenseInput.getText().toString().trim();
        String assignedVehicle = vehicleDropdown.getSelectedItem().toString();
        String assignedRoute = routeDropdown.getSelectedItem().toString();

        if (staffName.isEmpty() || staffPhone.isEmpty() ||  staffLicense.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create staff data object
        Map<String, Object> staffData = new HashMap<>();
        staffData.put("staffName", staffName);
        staffData.put("staffPhone", staffPhone);
        staffData.put("staffLicense", staffLicense);
        staffData.put("assignedVehicle", assignedVehicle);
        staffData.put("assignedRoute", assignedRoute);

        // Save data to Firestore
        db.collection("staff").add(staffData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Staff added successfully", Toast.LENGTH_SHORT).show();
            clearForm();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error saving staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

