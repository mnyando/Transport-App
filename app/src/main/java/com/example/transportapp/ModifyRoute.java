package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyRoute extends AppCompatActivity {

    private EditText routeNameInput, totalDistanceInput, routeDescriptionInput;
    private Spinner vehicleDropdown;
    private Button updateRouteButton;
    private RecyclerView routeRecyclerView;

    private FirebaseFirestore firestore;
    private List<Route> routeList;
    private RouteAdapter routeAdapter;
    private String selectedRouteId; // To track the selected route for updating

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_route);

        // Initialize UI elements
        routeNameInput = findViewById(R.id.routeNameInput);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        updateRouteButton = findViewById(R.id.updateRouteButton);
        routeRecyclerView = findViewById(R.id.routeRecyclerView);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        routeList = new ArrayList<>();
        routeAdapter = new RouteAdapter(routeList, this::onRouteSelected);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeRecyclerView.setAdapter(routeAdapter);

        // Fetch routes from Firestore
        fetchRoutes();

        // Set button click listener for updating route
        updateRouteButton.setOnClickListener(v -> updateRouteInFirestore());
    }

    private void fetchRoutes() {
        firestore.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Route route = document.toObject(Route.class);
                        route.setId(document.getId()); // Store document ID
                        routeList.add(route);
                    }
                    routeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch routes", Toast.LENGTH_SHORT).show());
    }

    private void onRouteSelected(Route route) {
        selectedRouteId = route.getId();
        routeNameInput.setText(route.getRouteName());
        totalDistanceInput.setText(String.valueOf(route.getTotalDistance()));
        routeDescriptionInput.setText(route.getRouteDescription());

        // Set Spinner selection (you may need to adapt this part based on your Spinner setup)
        // For simplicity, assuming vehicle name is directly set as Spinner selection
        for (int i = 0; i < vehicleDropdown.getCount(); i++) {
            if (vehicleDropdown.getItemAtPosition(i).toString().equals(route.getVehicle())) {
                vehicleDropdown.setSelection(i);
                break;
            }
        }
    }

    private void updateRouteInFirestore() {
        if (selectedRouteId == null) {
            Toast.makeText(this, "Please select a route to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String routeName = routeNameInput.getText().toString().trim();
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

        // Prepare updated data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("routeName", routeName);
        updatedData.put("totalDistance", totalDistance);
        updatedData.put("routeDescription", routeDescription);
        updatedData.put("vehicle", selectedVehicle);

        // Update Firestore
        firestore.collection("Routes")
                .document(selectedRouteId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Route updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update route", Toast.LENGTH_SHORT).show());
    }
}
