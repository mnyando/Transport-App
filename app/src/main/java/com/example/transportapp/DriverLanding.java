package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

public class DriverLanding extends AppCompatActivity {

    private TextView driverIdTextView, driverEmailTextView, driverNameTextView,
            vehicleTextView, routeTextView;
    private ImageView driverImageView;
    private Button pickUpButton, dropOffButton;
    private FirebaseFirestore db;
    private String driverId, driverAuthUid, driverEmail, driverName, assignedVehicle, assignedRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get all data from Intent
        Intent intent = getIntent();
        driverId = intent.getStringExtra("driverId");
        driverEmail = intent.getStringExtra("driverEmail");
        driverName = intent.getStringExtra("driverName");
        assignedVehicle = intent.getStringExtra("assignedVehicle");
        assignedRoute = intent.getStringExtra("assignedRoute");

        if (driverId == null ) {
            Toast.makeText(this, "Driver information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize all views
        initializeViews();

        // Set the driver information
        displayDriverInfo();

        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        driverIdTextView = findViewById(R.id.driverIdTextView);
        driverEmailTextView = findViewById(R.id.driverEmailTextView);
        driverNameTextView = findViewById(R.id.driverNameTextView);
        vehicleTextView = findViewById(R.id.vehicleTextView);
        routeTextView = findViewById(R.id.routeTextView);
        driverImageView = findViewById(R.id.driverImageView);
        pickUpButton = findViewById(R.id.pickUpButton);
        dropOffButton = findViewById(R.id.dropOffButton);
    }

    private void displayDriverInfo() {
        // Set all the driver information in the appropriate TextViews
        driverIdTextView.setText("Driver ID: " + (driverId != null ? driverId : "N/A"));
        driverEmailTextView.setText("Email: " + (driverEmail != null ? driverEmail : "N/A"));
        driverNameTextView.setText("Driver Name: " + (driverName != null ? driverName : "N/A"));
        vehicleTextView.setText("Assigned Vehicle: " + (assignedVehicle != null ? assignedVehicle : "N/A"));
        routeTextView.setText("Assigned Route: " + (assignedRoute != null ? assignedRoute : "N/A"));
    }

    private void setupButtonListeners() {
        pickUpButton.setOnClickListener(v -> {
            if (assignedVehicle == null || assignedVehicle.isEmpty()) {
                Toast.makeText(this, "No vehicle assigned to you", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start PickUp activity with necessary data
            Intent pickUpIntent = new Intent(DriverLanding.this, PickUp.class);
            pickUpIntent.putExtra("assignedVehicle", assignedVehicle);
            pickUpIntent.putExtra("driverId", driverId);
            pickUpIntent.putExtra("driverName", driverName);
            pickUpIntent.putExtra("assignedRoute", assignedRoute);
            startActivity(pickUpIntent);
        });

        dropOffButton.setOnClickListener(v -> {
            if (assignedVehicle == null || assignedVehicle.isEmpty()) {
                Toast.makeText(this, "No vehicle assigned to you", Toast.LENGTH_SHORT).show();
                return;
            }

            //Start DropOff activity (similar to PickUp)
            Intent dropOffIntent = new Intent(DriverLanding.this, DropOff.class);
            dropOffIntent.putExtra("assignedVehicle", assignedVehicle);
            dropOffIntent.putExtra("driverId", driverId);
            dropOffIntent.putExtra("driverName", driverName);
            dropOffIntent.putExtra("assignedRoute", assignedRoute);
            startActivity(dropOffIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh driver info in case it was updated
        refreshDriverInfo();
    }

    private void refreshDriverInfo() {
        if (driverId != null) {
            db.collection("drivers").document(driverId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            assignedVehicle = documentSnapshot.getString("assignedVehicle");
                            assignedRoute = documentSnapshot.getString("assignedRoute");
                            displayDriverInfo();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to refresh driver info", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}