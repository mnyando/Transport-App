package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddRoute extends AppCompatActivity {

    private EditText routeNameEditText;
    private EditText totalDistanceInput;
    private EditText routeDescriptionInput;
    private Spinner vehicleDropdown;
    private Button addRouteButton;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route); // Ensure this matches your XML filename

        // Initialize UI elements
        routeNameEditText = findViewById(R.id.routeNameEditText);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        addRouteButton = findViewById(R.id.addRouteButton);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Set button click listener
        addRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRouteToFirestore();
            }
        });
    }

    private void addRouteToFirestore() {
        String routeName = routeNameEditText.getText().toString().trim();
        String totalDistanceStr = totalDistanceInput.getText().toString().trim();
        String routeDescription = routeDescriptionInput.getText().toString().trim();
        String selectedVehicle = vehicleDropdown.getSelectedItem().toString();

        if (routeName.isEmpty() || totalDistanceStr.isEmpty() || routeDescription.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalDistance;
        try {
            totalDistance = Double.parseDouble(totalDistanceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to be sent to Firestore
        Map<String, Object> routeData = new HashMap<>();
        routeData.put("routeName", routeName);
        routeData.put("totalDistance", totalDistance);
        routeData.put("routeDescription", routeDescription);
        routeData.put("vehicle", selectedVehicle);

        // Add the route data to Firestore
        firestore.collection("Routes")
                .add(routeData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Route added successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        routeNameEditText.setText("");
        totalDistanceInput.setText("");
        routeDescriptionInput.setText("");
        vehicleDropdown.setSelection(0);
    }
}
