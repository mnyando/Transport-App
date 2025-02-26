package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteVehicle extends AppCompatActivity {

    private EditText vehicleIdInput;
    private Button deleteVehicleButton;
    private ImageButton backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_vehicle);

        db = FirebaseFirestore.getInstance();

        vehicleIdInput = findViewById(R.id.vehicleIdInput);
        deleteVehicleButton = findViewById(R.id.deleteVehicleButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish()); // Go back

        deleteVehicleButton.setOnClickListener(v -> deleteVehicleData());
    }

    private void deleteVehicleData() {
        String vehicleId = vehicleIdInput.getText().toString().trim();

        if (vehicleId.isEmpty()) {
            Toast.makeText(this, "Please enter Vehicle ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("vehicle").document(vehicleId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
