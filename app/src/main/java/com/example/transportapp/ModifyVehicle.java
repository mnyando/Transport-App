package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ModifyVehicle extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference vehiclesRef;
    private EditText vehicleNameInput, vehicleNumberInput, vehicleIdInput, capacityInput;
    private RadioGroup statusRadioGroup; // Added RadioGroup
    private Button saveVehicleButton;
    private RecyclerView vehicleRecyclerView;
    private VehicleAdapter vehicleAdapter;
    private List<Vehicle> vehicleList;
    private String selectedVehicleId = null; // Track editing vehicle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_vehicle);

        db = FirebaseFirestore.getInstance();
        vehiclesRef = db.collection("vehicle");

        // Initialize Views
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        capacityInput = findViewById(R.id.capacityInput);
        statusRadioGroup = findViewById(R.id.statusRadioGroup); // Initialize RadioGroup
        saveVehicleButton = findViewById(R.id.saveVehicleButton);
        vehicleRecyclerView = findViewById(R.id.vehicleRecyclerView);

        // Setup RecyclerView
        vehicleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vehicleList = new ArrayList<>();
        vehicleAdapter = new VehicleAdapter(vehicleList, this::onVehicleClicked);
        vehicleRecyclerView.setAdapter(vehicleAdapter);

        // Fetch vehicles
        fetchVehicles();

        // Save Button Click
        saveVehicleButton.setOnClickListener(v -> saveVehicleData());
    }

    private void fetchVehicles() {
        vehiclesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Vehicle vehicle = document.toObject(Vehicle.class);
                vehicle.setId(document.getId());
                vehicleList.add(vehicle);
            }
            vehicleAdapter.notifyDataSetChanged();
        });
    }

    private void onVehicleClicked(Vehicle vehicle) {
        selectedVehicleId = vehicle.getId();
        vehicleNameInput.setText(vehicle.getVehicleName());
        vehicleNumberInput.setText(vehicle.getVehicleNumber());
        vehicleIdInput.setText(vehicle.getId());
        capacityInput.setText(vehicle.getCapacity());
        // Set radio button based on vehicle status
        if ("Active".equals(vehicle.getStatus())) {
            statusRadioGroup.check(R.id.activeRadioButton);
        } else if ("Not Active".equals(vehicle.getStatus())) {
            statusRadioGroup.check(R.id.notActiveRadioButton);
        } else {
            statusRadioGroup.clearCheck(); // Clear selection if status is null or unexpected
        }
    }

    private void saveVehicleData() {
        if (selectedVehicleId == null) {
            Toast.makeText(this, "Please select a vehicle to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String vehicleName = vehicleNameInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String capacity = capacityInput.getText().toString().trim();
        int selectedStatusId = statusRadioGroup.getCheckedRadioButtonId();
        String status;

        if (vehicleName.isEmpty() || vehicleNumber.isEmpty() || capacity.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine selected status
        if (selectedStatusId == R.id.activeRadioButton) {
            status = "Active";
        } else if (selectedStatusId == R.id.notActiveRadioButton) {
            status = "Not Active";
        } else {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference vehicleRef = vehiclesRef.document(selectedVehicleId);
        vehicleRef.update(
                        "vehicleName", vehicleName,
                        "vehicleNumber", vehicleNumber,
                        "capacity", capacity,
                        "status", status // Add status to update
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show();
                    // Clear the inputs after successful update
                    vehicleNameInput.setText("");
                    vehicleNumberInput.setText("");
                    vehicleIdInput.setText("");
                    capacityInput.setText("");
                    statusRadioGroup.clearCheck(); // Clear radio selection
                    selectedVehicleId = null;
                    // Refresh the vehicle list
                    fetchVehicles();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}