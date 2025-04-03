package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class PickUp extends AppCompatActivity {
    private static final String TAG = "PickUpActivity";

    private RecyclerView studentListRecyclerView;
    private Button tripCompleteButton;
    private FirebaseFirestore db;
    private String assignedVehicle, tripId, driverId, driverName, assignedRoute;
    private List<Student> students;
    private StudentStatusAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_up);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data from Intent
        Intent intent = getIntent();
        assignedVehicle = intent.getStringExtra("assignedVehicle");
        driverId = intent.getStringExtra("driverId");
        driverName = intent.getStringExtra("driverName");
        assignedRoute = intent.getStringExtra("assignedRoute");

        if (assignedVehicle == null || assignedVehicle.isEmpty()) {
            showToastAndFinish("Vehicle information not found");
            return;
        }

        if (driverId == null || driverId.isEmpty()) {
            showToastAndFinish("Driver information not found");
            return;
        }

        initializeViews();
        loadStudentsForVehicle();
    }

    private void initializeViews() {
        studentListRecyclerView = findViewById(R.id.studentListRecyclerView);
        tripCompleteButton = findViewById(R.id.tripCompleteButton);

        students = new ArrayList<>();
        adapter = new StudentStatusAdapter(students, "", true); // Initialize with empty tripId
        studentListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentListRecyclerView.setAdapter(adapter);

        tripCompleteButton.setOnClickListener(v -> completeTrip());
    }

    private void loadStudentsForVehicle() {
        Log.d(TAG, "Loading students for vehicle: " + assignedVehicle);

        db.collectionGroup("studentList")
                .whereEqualTo("vehicle", assignedVehicle)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        students.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Student student = document.toObject(Student.class);
                                student.setId(document.getId());

                                // Extract grade from document path
                                String[] pathParts = document.getReference().getPath().split("/");
                                if (pathParts.length >= 2) {
                                    String grade = pathParts[1];
                                    student.setGrade(grade);
                                }

                                students.add(student);
                                Log.d(TAG, "Added student: " + student.getName());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing student document", e);
                            }
                        }

                        if (students.isEmpty()) {
                            showToast("No students assigned to this vehicle");
                        } else {
                            adapter.notifyDataSetChanged();
                            createNewTrip();
                        }
                    } else {
                        Log.e(TAG, "Error loading students", task.getException());
                        showToast("Error loading students: " + task.getException().getMessage());
                    }
                });
    }

    private void createNewTrip() {
        long currentTimeMillis = System.currentTimeMillis();
        String formattedTime = formatTimestamp(currentTimeMillis);

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("vehicle", assignedVehicle);
        tripData.put("driverId", driverId);
        tripData.put("driverName", driverName);
        tripData.put("route", assignedRoute);
        tripData.put("startTimeMillis", currentTimeMillis);
        tripData.put("startTimeFormatted", formattedTime);
        tripData.put("completed", false);
        tripData.put("studentCount", students.size());

        db.collection("trips")
                .add(tripData)
                .addOnSuccessListener(documentReference -> {
                    tripId = documentReference.getId();
                    adapter.setTripId(tripId);

                    Log.i(TAG, "Created new trip: " + tripId);
                    showToast("Trip started at " + formattedTime);

                    // Update all students with trip ID
                    updateStudentsWithTripId();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create trip", e);
                    showToast("Failed to start trip: " + e.getMessage());
                });
    }

    private void updateStudentsWithTripId() {
        for (Student student : students) {
            db.collection("students")
                    .document(student.getGrade())
                    .collection("studentList")
                    .document(student.getId())
                    .update("currentTripId", tripId)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update student trip ID: " + student.getId(), e);
                    });
        }
    }

    private void completeTrip() {
        if (tripId == null) {
            showToast("Trip not initialized");
            return;
        }

        long endTimeMillis = System.currentTimeMillis();
        String formattedEndTime = formatTimestamp(endTimeMillis);

        db.collection("trips").document(tripId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long startMillis = documentSnapshot.getLong("startTimeMillis");
                        String duration = (startMillis != null) ?
                                formatDuration(startMillis, endTimeMillis) : "N/A";

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("endTimeMillis", endTimeMillis);
                        updates.put("endTimeFormatted", formattedEndTime);
                        updates.put("completed", true);
                        updates.put("durationMillis", endTimeMillis - startMillis);
                        updates.put("durationFormatted", duration);

                        db.collection("trips").document(tripId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.i(TAG, "Trip completed: " + tripId);
                                    showToast("Trip completed at " + formattedEndTime + "\nDuration: " + duration);
                                    sendNotificationToParents();
                                    clearStudentsTripId();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to complete trip", e);
                                    showToast("Failed to complete trip: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get trip details", e);
                    showToast("Error completing trip");
                });
    }

    private void sendNotificationToParents() {
        for (Student student : students) {
            String parentId = student.getParentId(); // Assuming you have a parentId in the Student object

            // Create a notification for the parent
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("parentId", parentId);
            notificationData.put("message", "Your child's trip has been completed.");
            notificationData.put("driverId", driverId);
            notificationData.put("timestamp", System.currentTimeMillis());

            // Save the notification in Firestore
            db.collection("notifications").add(notificationData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Notification sent to parent: " + parentId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send notification to parent", e);
                    });
        }
    }

    private void clearStudentsTripId() {
        for (Student student : students) {
            db.collection("students")
                    .document(student.getGrade())
                    .collection("studentList")
                    .document(student.getId())
                    .update("currentTripId", null)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to clear student trip ID: " + student.getId(), e);
                    });
        }
    }

    // Helper methods
    private String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy 'at' hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(millis));
    }

    private String formatDuration(long startMillis, long endMillis) {
        long duration = endMillis - startMillis;
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hrs %02d mins", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d mins", minutes);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToastAndFinish(String message) {
        showToast(message);
        finish();
    }
}
