package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddVehicle extends AppCompatActivity {

    private EditText vehicleNameInput, vehicleNumberInput, capacityInput;
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
        saveVehicleButton = findViewById(R.id.saveVehicleButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish()); // Go back
        saveVehicleButton.setOnClickListener(v -> saveVehicleData()); // Save vehicle
    }

    private void saveVehicleData() {
        String vehicleName = vehicleNameInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String capacity = capacityInput.getText().toString().trim();

        if (vehicleName.isEmpty() || vehicleNumber.isEmpty() || capacity.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map for the vehicle data
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("vehicleName", vehicleName);
        vehicleData.put("vehicleNumber", vehicleNumber);
        vehicleData.put("capacity", capacity);

        // Save data to Firestore
        db.collection("vehicle").add(vehicleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Vehicle added successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
