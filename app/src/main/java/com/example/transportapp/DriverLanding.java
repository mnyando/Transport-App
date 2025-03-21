package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private List<Notification> notificationList; // Correct type: List<Notification>
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
        notificationList = new ArrayList<>(); // Initialize as List<Notification>
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationRecyclerView.setAdapter(notificationAdapter);

        // Load data
        loadDriverData();
        loadNotifications();
    }

    private void loadDriverData() {
        db.collection("drivers").document(driverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Driver driver = documentSnapshot.toObject(Driver.class);
                        if (driver != null) {
                            driverNameTextView.setText("Driver Name: " + driver.getDriverName());
                            vehicleTextView.setText("Assigned Vehicle: " + driver.getAssignedVehicle());
                            routeTextView.setText("Assigned Route: " + driver.getAssignedRoute());
                        }
                    } else {
                        Toast.makeText(this, "Driver data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading driver data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("driverId", driverId)
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




