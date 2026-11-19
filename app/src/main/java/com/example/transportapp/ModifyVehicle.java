package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

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
    /** Name as loaded: routes, staff and students point at the bus by this name. */
    private String selectedVehicleName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_vehicle);

        db = FirebaseFirestore.getInstance();
        vehiclesRef = db.collection(FirestorePaths.VEHICLES);

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

        // The document ID is shown for reference only; it can't be changed here.
        vehicleIdInput.setEnabled(false);

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
        }).addOnFailureListener(e -> UserErrors.show(this, "Could not load vehicles", e));
    }

    private void onVehicleClicked(Vehicle vehicle) {
        selectedVehicleId = vehicle.getId();
        selectedVehicleName = vehicle.getVehicleName();
        vehicleNameInput.setText(vehicle.getVehicleName());
        vehicleNumberInput.setText(vehicle.getVehicleNumber());
        vehicleIdInput.setText(vehicle.getId());
        capacityInput.setText(vehicle.capacityText());
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

        // Capacity is saved as a number so it can be compared safely (review #13).
        int seats = Vehicle.parseCapacity(capacity);
        if (seats <= 0) {
            capacityInput.setError(getString(R.string.error_capacity_number));
            capacityInput.requestFocus();
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

        String vehicleId = selectedVehicleId;
        String oldName = selectedVehicleName != null ? selectedVehicleName : vehicleName;
        boolean renaming = !vehicleName.equals(oldName);

        // Before saving: the new name must be free; a bus that is in use can't be renamed (its
        // routes, staff, children and bus group all hold the old name); and the capacity can't
        // drop below the children already riding it.
        Task<Boolean> nameCheck = renaming
                ? RecordLinks.nameTaken(db, FirestorePaths.VEHICLES, "vehicleName", vehicleName, vehicleId)
                : Tasks.forResult(false);
        Task<Long> children = RecordLinks.studentsOnVehicle(db, oldName);
        Task<Long> routes = renaming ? RecordLinks.routesForVehicle(db, oldName) : Tasks.forResult(0L);
        Task<Long> staff = renaming ? RecordLinks.staffWith(db, "assignedVehicle", oldName) : Tasks.forResult(0L);

        saveVehicleButton.setEnabled(false);
        Tasks.whenAllComplete(nameCheck, children, routes, staff).addOnCompleteListener(all -> {
            if (!nameCheck.isSuccessful() || !children.isSuccessful()
                    || !routes.isSuccessful() || !staff.isSuccessful()) {
                saveVehicleButton.setEnabled(true);
                UserErrors.show(this, getString(R.string.checking_links_failed), null);
                return;
            }
            if (Boolean.TRUE.equals(nameCheck.getResult())) {
                saveVehicleButton.setEnabled(true);
                vehicleNameInput.setError(getString(R.string.error_vehicle_name_taken, vehicleName));
                vehicleNameInput.requestFocus();
                return;
            }
            long riders = children.getResult();
            if (renaming && (riders > 0 || routes.getResult() > 0 || staff.getResult() > 0)) {
                saveVehicleButton.setEnabled(true);
                Toast.makeText(this, getString(R.string.error_vehicle_rename_in_use,
                        routes.getResult().intValue(), staff.getResult().intValue(), (int) riders),
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (seats < riders) {
                saveVehicleButton.setEnabled(true);
                capacityInput.setError(getResources().getQuantityString(R.plurals.error_capacity_below_students, (int) riders, (int) riders));
                capacityInput.requestFocus();
                return;
            }
            updateVehicle(vehicleId, vehicleName, vehicleNumber, seats, status);
        });
    }

    private void updateVehicle(String vehicleId, String vehicleName, String vehicleNumber,
                               int seats, String status) {
        DocumentReference vehicleRef = vehiclesRef.document(vehicleId);
        vehicleRef.update(
                        "vehicleName", vehicleName,
                        "vehicleNumber", vehicleNumber,
                        "capacity", (long) seats,
                        "status", status // Add status to update
                )
                .addOnSuccessListener(aVoid -> {
                    saveVehicleButton.setEnabled(true);
                    Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show();
                    // Clear the inputs after successful update
                    vehicleNameInput.setText("");
                    vehicleNumberInput.setText("");
                    vehicleIdInput.setText("");
                    capacityInput.setText("");
                    statusRadioGroup.clearCheck(); // Clear radio selection
                    selectedVehicleId = null;
                    selectedVehicleName = null;
                    // Refresh the vehicle list
                    fetchVehicles();
                })
                .addOnFailureListener(e -> {
                    saveVehicleButton.setEnabled(true);
                    UserErrors.show(this, "Could not update vehicle", e);
                });
    }
}