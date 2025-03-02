package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
    private Spinner filterDropdown;
    private EditText vehicleNameInput, vehicleNumberInput, vehicleIdInput, capacityInput;
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
        filterDropdown = findViewById(R.id.filterDropdown);
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        capacityInput = findViewById(R.id.capacityInput);
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
    }

    private void saveVehicleData() {
        DocumentReference vehicleRef = vehiclesRef.document(selectedVehicleId);
        vehicleRef.update("vehicleName", vehicleNameInput.getText().toString(),
                "vehicleNumber", vehicleNumberInput.getText().toString(),
                "capacity", capacityInput.getText().toString());
    }
}
