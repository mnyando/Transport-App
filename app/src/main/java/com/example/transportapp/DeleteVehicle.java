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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
    private String selectedVehicleName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_vehicle);

        db = FirebaseFirestore.getInstance();
        vehiclesRef = db.collection(FirestorePaths.VEHICLES);

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

        // This screen only shows the details; edits are made on Modify Vehicle.
        for (EditText field : new EditText[]{vehicleNameInput, vehicleNumberInput, vehicleIdInput, capacityInput}) {
            field.setEnabled(false);
        }

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
                    UserErrors.show(this, "Could not load vehicles", e);
                });
    }

    private void onVehicleClicked(Vehicle vehicle) {
        selectedVehicleId = vehicle.getId();
        selectedVehicleName = vehicle.getVehicleName();
        vehicleNameInput.setText(vehicle.getVehicleName());
        vehicleNumberInput.setText(vehicle.getVehicleNumber());
        vehicleIdInput.setText(vehicle.getId());
        capacityInput.setText(vehicle.capacityText());
    }

    private void showDeleteConfirmationDialog() {
        if (selectedVehicleId == null) {
            Toast.makeText(this, "Please select a vehicle to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Routes, staff and children point at the bus by name; removing it would strand them.
        String name = selectedVehicleName != null ? selectedVehicleName : "";
        Task<Long> routes = RecordLinks.routesForVehicle(db, name);
        Task<Long> staff = RecordLinks.staffWith(db, "assignedVehicle", name);
        Task<Long> children = RecordLinks.studentsOnVehicle(db, name);
        deleteVehicleButton.setEnabled(false);
        Tasks.whenAllComplete(routes, staff, children).addOnCompleteListener(t -> {
            deleteVehicleButton.setEnabled(true);
            if (!routes.isSuccessful() || !staff.isSuccessful() || !children.isSuccessful()) {
                UserErrors.show(this, getString(R.string.checking_links_failed), null);
                return;
            }
            long r = routes.getResult(), s = staff.getResult(), c = children.getResult();
            if (r > 0 || s > 0 || c > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Vehicle Record")
                        .setMessage(getString(R.string.error_vehicle_delete_in_use, (int) r, (int) s, (int) c))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete Vehicle Record")
                    .setMessage("Are you sure you want to delete this vehicle?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteVehicleRecord())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteVehicleRecord() {
        if (selectedVehicleId == null) return;
        deleteVehicleButton.setEnabled(false);
        DocumentReference vehicleRef = vehiclesRef.document(selectedVehicleId);
        vehicleRef.delete()
                .addOnSuccessListener(aVoid -> {
                    deleteVehicleButton.setEnabled(true);
                    Toast.makeText(this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show();
                    // Clear inputs
                    vehicleNameInput.setText("");
                    vehicleNumberInput.setText("");
                    vehicleIdInput.setText("");
                    capacityInput.setText("");
                    selectedVehicleId = null;
                    selectedVehicleName = null;
                    // Refresh list
                    fetchVehicles();
                })
                .addOnFailureListener(e -> {
                    deleteVehicleButton.setEnabled(true);
                    UserErrors.show(this, "Could not delete vehicle", e);
                });
    }
}