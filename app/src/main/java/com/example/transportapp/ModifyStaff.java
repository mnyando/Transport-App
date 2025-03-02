package com.example.transportapp;

import android.os.Bundle;
import android.text.TextUtils;
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

public class ModifyStaff extends AppCompatActivity implements DriverAdapter.OnDriverClickListener {

    private EditText driverNameInput, driverPhoneInput, licenseNumberInput;
    private Spinner vehicleDropdown, routeDropdown;
    private Button updateDriverButton;
    private RecyclerView driverRecyclerView;
    private DriverAdapter driverAdapter;
    private List<Driver> driverList;
    private FirebaseFirestore firestore;
    private String selectedDriverId = null;
    private List<String> vehicleList;
    private List<String> routeList;
    private ArrayAdapter<String> vehicleAdapter;
    private ArrayAdapter<String> routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        driverNameInput = findViewById(R.id.driverNameInput);
        driverPhoneInput = findViewById(R.id.driverPhoneInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        updateDriverButton = findViewById(R.id.saveDriverButton);
        driverRecyclerView = findViewById(R.id.driverRecyclerView);

        // Setup RecyclerView
        driverList = new ArrayList<>();
        driverAdapter = new DriverAdapter(driverList, this);
        driverRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        driverRecyclerView.setAdapter(driverAdapter);

        // Setup Spinners
        vehicleList = new ArrayList<>();
        routeList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleList);
        routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);
        routeDropdown.setAdapter(routeAdapter);

        // Load data
        fetchVehicles();
        fetchRoutes();
        fetchDrivers();

        // Handle driver updates
        updateDriverButton.setOnClickListener(v -> updateDriver());
    }

    private void fetchDrivers() {
        firestore.collection("staff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    driverList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Driver driver = doc.toObject(Driver.class);
                        driver.setId(doc.getId());
                        driverList.add(driver);
                    }
                    driverAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching drivers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchVehicles() {
        firestore.collection("vehicle")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String vehicleName = doc.getString("vehicleName");
                        if (vehicleName != null) {
                            vehicleList.add(vehicleName);
                        }
                    }
                    vehicleAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchRoutes() {
        firestore.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String routeName = doc.getString("routeName");
                        if (routeName != null) {
                            routeList.add(routeName);
                        }
                    }
                    routeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDriverClick(Driver driver) {
        selectedDriverId = driver.getId();
        driverNameInput.setText(driver.getStaffName());
        driverPhoneInput.setText(driver.getStaffPhone());
        licenseNumberInput.setText(driver.getStaffLicense());

        // Set Spinner selections
        int vehiclePosition = vehicleAdapter.getPosition(driver.getAssignedVehicle());
        if (vehiclePosition >= 0) {
            vehicleDropdown.setSelection(vehiclePosition);
        } else {
            vehicleDropdown.setSelection(0);
        }

        int routePosition = routeAdapter.getPosition(driver.getAssignedRoute());
        if (routePosition >= 0) {
            routeDropdown.setSelection(routePosition);
        } else {
            routeDropdown.setSelection(0);
        }
    }

    private void updateDriver() {
        if (selectedDriverId == null) {
            Toast.makeText(this, "Please select a driver to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedName = driverNameInput.getText().toString().trim();
        String updatedPhone = driverPhoneInput.getText().toString().trim();
        String updatedLicense = licenseNumberInput.getText().toString().trim();
        String updatedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String updatedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(updatedName) || TextUtils.isEmpty(updatedPhone) || TextUtils.isEmpty(updatedLicense) ||
                TextUtils.isEmpty(updatedVehicle) || TextUtils.isEmpty(updatedRoute)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("staffName", updatedName);
        updatedData.put("staffPhone", updatedPhone);
        updatedData.put("staffLicense", updatedLicense);
        updatedData.put("assignedVehicle", updatedVehicle);
        updatedData.put("assignedRoute", updatedRoute);

        firestore.collection("staff").document(selectedDriverId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Driver updated successfully", Toast.LENGTH_SHORT).show();
                    fetchDrivers(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update driver: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}