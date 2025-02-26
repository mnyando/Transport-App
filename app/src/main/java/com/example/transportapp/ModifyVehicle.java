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

public class ModifyVehicle extends AppCompatActivity {

    private EditText vehicleIdInput, vehicleNameInput, vehicleNumberInput, capacityInput;
    private Button updateVehicleButton;
    private ImageButton backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_vehicle);

        db = FirebaseFirestore.getInstance();

        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        capacityInput = findViewById(R.id.capacityInput);
        updateVehicleButton = findViewById(R.id.updateVehicleButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish()); // Go back

        updateVehicleButton.setOnClickListener(v -> updateVehicleData());
    }

    private void updateVehicleData() {
        String vehicleId = vehicleIdInput.getText().toString().trim();
        String vehicleName = vehicleNameInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String capacity = capacityInput.getText().toString().trim();

        if (vehicleId.isEmpty() || vehicleName.isEmpty() || vehicleNumber.isEmpty() || capacity.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("vehicleName", vehicleName);
        updatedData.put("vehicleNumber", vehicleNumber);
        updatedData.put("capacity", capacity);

        db.collection("vehicle").document(vehicleId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
