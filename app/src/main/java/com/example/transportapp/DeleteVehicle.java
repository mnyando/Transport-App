package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteVehicle extends AppCompatActivity {

    private EditText vehicleNameInput, vehicleNumberInput, vehicleIdInput, capacityInput;
    private Spinner filterDropdown;
    private Button deleteVehicleButton;
    private ImageButton backButton;
    private FirebaseFirestore db;
    private String vehicleId; // Firestore Document ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_vehicle);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data passed from previous activity
        vehicleId = getIntent().getStringExtra("vehicleId");

        // Initialize UI elements
        vehicleNameInput = findViewById(R.id.vehicleNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        capacityInput = findViewById(R.id.capacityInput);
        filterDropdown = findViewById(R.id.filterDropdown);
        deleteVehicleButton = findViewById(R.id.deleteVehicleButton);
        backButton = findViewById(R.id.backButton);

        // Populate fields with received data
        vehicleNameInput.setText(getIntent().getStringExtra("vehicleName"));
        vehicleNumberInput.setText(getIntent().getStringExtra("vehicleNumber"));
        vehicleIdInput.setText(getIntent().getStringExtra("vehicleId"));
        capacityInput.setText(getIntent().getStringExtra("capacity"));

        backButton.setOnClickListener(v -> finish()); // Go back
        deleteVehicleButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Vehicle Record")
                .setMessage("Are you sure you want to delete this vehicle?")
                .setPositiveButton("Yes", (dialog, which) -> deleteVehicleRecord())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVehicleRecord() {
        DocumentReference vehicleRef = db.collection("vehicle").document(vehicleId);

        vehicleRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(DeleteVehicle.this, "Vehicle record deleted successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity after deletion
            } else {
                Toast.makeText(DeleteVehicle.this, "Error deleting record!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
