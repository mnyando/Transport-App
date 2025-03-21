package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentLanding extends AppCompatActivity {

    private static final String TAG = "ParentLanding";

    private RecyclerView childRecyclerView;
    private RecyclerView notificationRecyclerView;
    private List<String> childList;
    private List<Notification> notificationList; // Updated to List<Notification>
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter; // Using the NotificationAdapter with click listener
    private FirebaseFirestore db;
    private String parentId; // Parent ID passed from Login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get parent ID from Intent (assumed from Login)
        parentId = getIntent().getStringExtra("parentId");
        if (parentId == null) {
            Log.e(TAG, "Parent ID not found");
            finish();
            return;
        }

        // Initialize RecyclerViews
        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);

        // Initialize data lists
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();

        // Fetch data from Firestore
        fetchChildrenFromFirestore();
        fetchNotificationsFromFirestore();

        // Set up RecyclerViews
        setupRecyclerViews();
    }

    private void fetchChildrenFromFirestore() {
        db.collection("children")
                .whereEqualTo("parentId", parentId) // Filter by parent ID
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String childData = document.getString("name") + " - " + document.getString("grade");
                        childList.add(childData);
                    }
                    childAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.d(TAG, "Error getting children data: ", e));
    }

    private void fetchNotificationsFromFirestore() {
        db.collection("notifications")
                .whereEqualTo("parentId", parentId) // Filter by parent ID
                .orderBy("timestamp", Query.Direction.DESCENDING) // Latest first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notificationList.add(notification);
                    }
                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.d(TAG, "Error getting notifications: ", e));
    }

    private void setupRecyclerViews() {
        // Set up child RecyclerView
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);

        // Set up notification RecyclerView
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick); // Updated with click listener
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    private void onNotificationClick(Notification notification) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("driverId", notification.getDriverId()); // Navigate to chat with driver
        startActivity(intent);
    }
}




