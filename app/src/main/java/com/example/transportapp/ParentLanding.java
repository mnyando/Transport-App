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
    private List<Notification> notificationList;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;
    private FirebaseFirestore db;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get parent ID from Intent
        parentId = getIntent().getStringExtra("parentId");
        if (parentId == null || parentId.isEmpty()) {
            Log.e(TAG, "Parent ID not found");
            finish();
            return;
        }

        // Initialize RecyclerViews
        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);

        // Initialize lists
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();

        // Set up RecyclerViews before fetching data
        setupRecyclerViews();

        // Fetch data from Firestore
        fetchChildrenFromFirestore();
        fetchNotificationsFromFirestore();
    }

    private void setupRecyclerViews() {
        // Child RecyclerView setup
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);

        // Notification RecyclerView setup
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick);
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    private void fetchChildrenFromFirestore() {
        db.collection("children")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String childName = document.getString("name");
                        String childGrade = document.getString("grade");

                        if (childName != null && childGrade != null) {
                            String childData = childName + " - " + childGrade;
                            childList.add(childData);
                        }
                    }
                    childAdapter.notifyDataSetChanged(); // Notify adapter after data is updated
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting children data", e));
    }

    private void fetchNotificationsFromFirestore() {
        db.collection("notifications")
                .whereEqualTo("parentId", parentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notificationList.add(notification);
                    }
                    notificationAdapter.notifyDataSetChanged(); // Notify adapter after data is updated
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting notifications", e));
    }

    private void onNotificationClick(Notification notification) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("driverId", notification.getDriverId());
        startActivity(intent);
    }
}
