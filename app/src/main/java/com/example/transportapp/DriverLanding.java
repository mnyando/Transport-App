package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DriverLanding extends AppCompatActivity {

    private TextView driverNameTextView, vehicleTextView, routeTextView;
    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get driver ID from Intent
        driverId = getIntent().getStringExtra("driverId");
        if (driverId == null) {
            Toast.makeText(this, "Driver ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        driverNameTextView = findViewById(R.id.driverNameTextView);
        vehicleTextView = findViewById(R.id.vehicleTextView);
        routeTextView = findViewById(R.id.routeTextView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);

        // Setup RecyclerView for Notifications
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationRecyclerView.setAdapter(notificationAdapter);

        // Load data
        loadDriverData();
    }

    private void loadDriverData() {
        db.collection("staff").document(driverId) // Changed from "drivers" to "staff"
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String driverName = documentSnapshot.getString("staffName");
                        String assignedVehicle = documentSnapshot.getString("assignedVehicle");
                        String assignedRoute = documentSnapshot.getString("assignedRoute");

                        driverNameTextView.setText("Driver Name: " + driverName);
                        vehicleTextView.setText("Assigned Vehicle: " + assignedVehicle);
                        routeTextView.setText("Assigned Route: " + assignedRoute);

                        loadNotifications(driverId); // Load notifications after retrieving driver info
                    } else {
                        Toast.makeText(this, "Driver data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading driver data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadNotifications(String driverId) {
        db.collection("notifications")
                .whereEqualTo("driverId", driverId) // Ensure this matches how driverId is stored
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setId(doc.getId());
                        notificationList.add(notification);
                    }
                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onNotificationClick(Notification notification) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("parentId", notification.getParentId());
        intent.putExtra("driverId", driverId);
        startActivity(intent);
    }
}
