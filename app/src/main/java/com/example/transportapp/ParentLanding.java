package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ParentLanding extends AppCompatActivity {

    private static final String TAG = "ParentLanding";

    private RecyclerView childRecyclerView, notificationRecyclerView;
    private List<Student> childList;
    private List<Notification> notificationList;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;
    private FirebaseFirestore db;
    private String parentId, parentName;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        db = FirebaseFirestore.getInstance();
        parentId = getIntent().getStringExtra("parentId");

        if (parentId == null) {
            Log.e(TAG, "❌ ERROR: Parent ID not found! Exiting...");
            finish();
            return;
        }
        Log.d(TAG, "✅ Parent ID received: " + parentId);

        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();

        setupRecyclerViews();
        fetchParentName();
    }

    private void fetchParentName() {
        Log.d(TAG, "📡 Fetching parent name for parentId: " + parentId);
        db.collection("users").document(parentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        parentName = documentSnapshot.getString("fullName");
                        if (parentName != null) {
                            Log.d(TAG, "✅ Parent name found: " + parentName);
                            fetchChildrenFromFirestore();
                            fetchNotificationsFromFirestore(); // Fetch notifications after parentName is available
                        } else {
                            Log.e(TAG, "❌ Parent name not found!");
                            finish();
                        }
                    } else {
                        Log.e(TAG, "❌ Parent document does not exist!");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ERROR fetching parent name: ", e);
                    finish();
                });
    }

    private void fetchChildrenFromFirestore() {
        Log.d(TAG, "📡 Searching for students with parentName: " + parentName);
        childList.clear();

        db.collectionGroup("studentList")
                .whereEqualTo("parentName", parentName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());

                        // Extract grade from document path: students/{grade}/studentList/{studentId}
                        String grade = document.getReference().getParent().getParent().getId();
                        student.setGrade(grade);

                        childList.add(student);
                        Log.d(TAG, "✅ Student loaded: " + student.getName() +
                                " (Grade: " + grade + ", ID: " + student.getId() + ")");
                    }

                    childAdapter.updateList(childList);
                    Log.d(TAG, "✅ Total students found: " + childList.size());

                    if (childList.isEmpty()) {
                        Toast.makeText(this, "No students found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ERROR fetching students: ", e);
                    Toast.makeText(this, "Error loading students", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchNotificationsFromFirestore() {
        Log.d(TAG, "📡 Fetching notifications for parentNamee: " + parentName);

        // Listen to real-time updates on notifications
        notificationListener = db.collection("notifications")
                .whereEqualTo("parentName", parentName)  // Changed to parentName
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ ERROR fetching notifications: ", e);
                        return;
                    }

                    notificationList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notificationList.add(notification);
                        Log.d(TAG, "✅ Notification received: " + notification.getMessage());
                    }

                    notificationAdapter.notifyDataSetChanged();
                });
    }

    private void setupRecyclerViews() {
        // Setup children RecyclerView
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);
        Log.d(TAG, "✅ Child RecyclerView set up successfully");

        // Setup notifications RecyclerView
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick);
        notificationRecyclerView.setAdapter(notificationAdapter);
        Log.d(TAG, "✅ Notification RecyclerView set up successfully");
    }

    private void onNotificationClick(Notification notification) {
        Log.d(TAG, "📩 Notification clicked: " + notification.getMessage());
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("driverId", notification.getDriverId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove(); // Remove listener to prevent memory leaks
        }
    }
}
