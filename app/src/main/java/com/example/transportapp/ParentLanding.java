package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        db.collection("students")
                .whereEqualTo("parentName", parentName) // Filter by parent name
                .get()
                .addOnSuccessListener(studentList -> {
                    if (studentList.isEmpty()) {
                        Log.w(TAG, "⚠ No students found for parent: " + parentName);
                        Toast.makeText(this, "No students found for " + parentName, Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot studentDoc : studentList) {
                        Student student = studentDoc.toObject(Student.class);
                        student.setId(studentDoc.getId()); // Ensure student ID is set
                        childList.add(student);
                        Log.d(TAG, "✅ Student loaded: " + student.getName() + " (ID: " + student.getId() + ")");
                    }

                    childAdapter.updateList(childList); // Update RecyclerView with fetched students
                    Log.d(TAG, "✅ Total students found: " + childList.size());
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ ERROR fetching students: ", e));
    }

    private void fetchNotificationsFromFirestore() {
        Log.d(TAG, "📡 Fetching notifications for parentId: " + parentId);
        db.collection("notifications")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notificationList.add(notification);
                        Log.d(TAG, "✅ Notification received: " + notification.getMessage());
                    }
                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ ERROR fetching notifications: ", e));
    }

    private void setupRecyclerViews() {
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);
        Log.d(TAG, "✅ Child RecyclerView set up successfully");

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
}
