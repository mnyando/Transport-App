package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddVehicle extends AppCompatActivity {

    private EditText vehicleNameInput, vehicleNumberInput, capacityInput;
    private RadioGroup statusRadioGroup; // Added RadioGroup
    private Button saveVehicleButton;
    private ImageButton backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        capacityInput = findViewById(R.id.capacityInput);
        statusRadioGroup = findViewById(R.id.statusRadioGroup); // Initialize RadioGroup
        saveVehicleButton = findViewById(R.id.saveVehicleButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish()); // Go back
        saveVehicleButton.setOnClickListener(v -> saveVehicleData()); // Save vehicle
    }

    private void saveVehicleData() {
        String vehicleName = vehicleNameInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String capacity = capacityInput.getText().toString().trim();

        // Get selected status from RadioGroup
        int selectedId = statusRadioGroup.getCheckedRadioButtonId();
        String status;
        if (selectedId == R.id.activeRadioButton) {
            status = "Active";
        } else if (selectedId == R.id.notActiveRadioButton) {
            status = "Not Active";
        } else {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return; // Exit if no status is selected
        }

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

        // Create a map for the vehicle data
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("vehicleName", vehicleName);
        vehicleData.put("vehicleNumber", vehicleNumber);
        vehicleData.put("capacity", (long) seats);
        vehicleData.put("studentCount", 0L);
        vehicleData.put("status", status); // Add status to the map

        // Save data to Firestore
        db.collection(FirestorePaths.VEHICLES).add(vehicleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Vehicle added successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> UserErrors.show(this, "Could not save the vehicle", e));
    }
}