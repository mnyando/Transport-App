package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteVehicle extends AppCompatActivity {

    private FirebaseFirestore db;
    private CollectionReference vehiclesRef;
    private EditText vehicleNameInput, vehicleNumberInput, vehicleIdInput, capacityInput;
    private Button deleteVehicleButton;
    private ImageButton backButton;
    private RecyclerView vehicleRecyclerView;
    private VehicleAdapter vehicleAdapter;
    private List<Vehicle> vehicleList;
    private String selectedVehicleId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_vehicle);

        db = FirebaseFirestore.getInstance();
        vehiclesRef = db.collection("vehicle");

        // Initialize Views
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        capacityInput = findViewById(R.id.capacityInput);
        deleteVehicleButton = findViewById(R.id.deleteVehicleButton);
        backButton = findViewById(R.id.backButton);
        vehicleRecyclerView = findViewById(R.id.vehicleRecyclerView);

        // Setup RecyclerView
        vehicleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vehicleList = new ArrayList<>();
        vehicleAdapter = new VehicleAdapter(vehicleList, this::onVehicleClicked);
        vehicleRecyclerView.setAdapter(vehicleAdapter);

        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        deleteVehicleButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Fetch vehicles
        fetchVehicles();
    }

    private void fetchVehicles() {
        vehiclesRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Vehicle vehicle = document.toObject(Vehicle.class);
                        vehicle.setId(document.getId());
                        vehicleList.add(vehicle);
                    }
                    vehicleAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onVehicleClicked(Vehicle vehicle) {
        selectedVehicleId = vehicle.getId();
        vehicleNameInput.setText(vehicle.getVehicleName());
        vehicleNumberInput.setText(vehicle.getVehicleNumber());
        vehicleIdInput.setText(vehicle.getId());
        capacityInput.setText(vehicle.getCapacity());
    }

    private void showDeleteConfirmationDialog() {
        if (selectedVehicleId == null) {
            Toast.makeText(this, "Please select a vehicle to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Vehicle Record")
                .setMessage("Are you sure you want to delete this vehicle?")
                .setPositiveButton("Yes", (dialog, which) -> deleteVehicleRecord())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVehicleRecord() {
        DocumentReference vehicleRef = vehiclesRef.document(selectedVehicleId);
        vehicleRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show();
                    // Clear inputs
                    vehicleNameInput.setText("");
                    vehicleNumberInput.setText("");
                    vehicleIdInput.setText("");
                    capacityInput.setText("");
                    selectedVehicleId = null;
                    // Refresh list
                    fetchVehicles();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}