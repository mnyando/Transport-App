package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DropOff extends AppCompatActivity {
    private static final String TAG = "DropOffActivity";
    private static final String TRIP_TYPE = "dropoff";

    // UI Components
    private RecyclerView studentListRecyclerView;
    private Button tripCompleteButton;

    // Firestore
    private FirebaseFirestore db;

    // Data
    private String assignedVehicle, tripId, driverId, driverName, assignedRoute;
    private List<Student> students = new ArrayList<>();
    private StudentStatusAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop_off);
        initializeFirestore();
        getIntentData();
        initializeViews();
        loadStudentsForVehicle();
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void getIntentData() {
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
        }
    }

    private void initializeViews() {
        studentListRecyclerView = findViewById(R.id.studentListRecyclerView);
        tripCompleteButton = findViewById(R.id.tripCompleteButton);

        adapter = new StudentStatusAdapter(students, "", false);
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
        Map<String, Object> tripData = createTripData(currentTimeMillis);

        db.collection("trips")
                .add(tripData)
                .addOnSuccessListener(documentReference -> {
                    tripId = documentReference.getId();
                    adapter.setTripId(tripId);
                    Log.i(TAG, "Created new trip: " + tripId);
                    showToast("Drop-off trip started at " + formatTimestamp(currentTimeMillis));
                    updateStudentsWithTripId();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create trip", e);
                    showToast("Failed to start trip: " + e.getMessage());
                });
    }

    private Map<String, Object> createTripData(long currentTimeMillis) {
        Map<String, Object> tripData = new HashMap<>();
        tripData.put("vehicle", assignedVehicle);
        tripData.put("driverId", driverId);
        tripData.put("driverName", driverName);
        tripData.put("route", assignedRoute);
        tripData.put("startTimeMillis", currentTimeMillis);
        tripData.put("startTimeFormatted", formatTimestamp(currentTimeMillis));
        tripData.put("completed", false);
        tripData.put("studentCount", students.size());
        tripData.put("tripType", TRIP_TYPE);
        return tripData;
    }

    private void completeTrip() {
        if (tripId == null) {
            showToast("Trip not initialized");
            return;
        }

        long endTimeMillis = System.currentTimeMillis();
        db.collection("trips").document(tripId)
                .get()
                .addOnSuccessListener(documentSnapshot -> processTripCompletion(documentSnapshot, endTimeMillis))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get trip details", e);
                    showToast("Error completing drop-off trip");
                });
    }

    private void processTripCompletion(DocumentSnapshot documentSnapshot, long endTimeMillis) {
        if (!documentSnapshot.exists()) {
            showToast("Trip document not found");
            return;
        }

        Long startMillis = documentSnapshot.getLong("startTimeMillis");
        String duration = (startMillis != null) ? formatDuration(startMillis, endTimeMillis) : "N/A";

        Map<String, Object> updates = createTripCompletionUpdates(endTimeMillis, startMillis, duration);

        db.collection("trips").document(tripId)
                .update(updates)
                .addOnSuccessListener(aVoid -> handleTripCompletionSuccess(endTimeMillis, duration))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to complete drop-off trip", e);
                    showToast("Failed to complete drop-off: " + e.getMessage());
                });
    }

    private Map<String, Object> createTripCompletionUpdates(long endTimeMillis, Long startMillis, String duration) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("endTimeMillis", endTimeMillis);
        updates.put("endTimeFormatted", formatTimestamp(endTimeMillis));
        updates.put("completed", true);
        if (startMillis != null) {
            updates.put("durationMillis", endTimeMillis - startMillis);
        }
        updates.put("durationFormatted", duration);
        return updates;
    }

    private void handleTripCompletionSuccess(long endTimeMillis, String duration) {
        Log.i(TAG, "Drop-off trip completed: " + tripId);
        showToast("Drop-off completed at " + formatTimestamp(endTimeMillis) + "\nDuration: " + duration);

        sendDropOffNotificationToParent();
        clearStudentsTripId();
        finish();
    }

    private void sendDropOffNotificationToParent() {
        for (Student student : students) {
            String parentName = student.getParentName();

            if (parentName == null || parentName.isEmpty()) {
                Log.e(TAG, "Parent name not found for student: " + student.getName());
                continue;
            }

            // 1. Save notification to Firestore (for in-app notifications)
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("parentName", parentName);
            notificationData.put("driverName", driverName);
            notificationData.put("message", "Your child " + student.getName() + " has been dropped off.");
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("type", "dropoff");

            db.collection("notifications").add(notificationData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Notification saved to Firestore for: " + parentName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save notification to Firestore", e);
                    });

            // 2. Send FCM push notification
            sendFCMPushNotification(parentName,
                    "Drop Off Complete",
                    "Your child " + student.getName() + " has been dropped off.");
        }
    }

    private void sendFCMPushNotification(String parentName, String title, String message) {
        db.collection("parents")
                .whereEqualTo("name", parentName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String parentToken = queryDocumentSnapshots.getDocuments().get(0).getString("deviceToken");
                        if (parentToken != null && !parentToken.isEmpty()) {
                            try {
                                FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(parentToken + "@fcm.googleapis.com")
                                        .setMessageId(Integer.toString(new Random().nextInt()))
                                        .addData("title", title)
                                        .addData("message", message)
                                        .addData("type", "dropoff")  // Add type for notification handling
                                        .build());
                                Log.d(TAG, "FCM notification sent to parent: " + parentName);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to send FCM notification", e);
                            }
                        } else {
                            Log.w(TAG, "No device token found for parent: " + parentName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching parent device token", e);
                });
    }

    private void sendNotificationToParent(String parentName, String title, String message) {
        db.collection("parents")
                .whereEqualTo("name", parentName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String parentToken = queryDocumentSnapshots.getDocuments().get(0).getString("deviceToken");
                        if (parentToken != null && !parentToken.isEmpty()) {
                            sendFCMNotification(parentToken, title, message);
                        } else {
                            Log.w(TAG, "No device token found for parent: " + parentName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching parent device token", e);
                });
    }

    private void sendFCMNotification(String deviceToken, String title, String message) {
        try {
            FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(deviceToken + "@fcm.googleapis.com")
                    .setMessageId(Integer.toString(new Random().nextInt()))
                    .addData("title", title)
                    .addData("message", message)
                    .build());
            Log.d(TAG, "Notification sent to parent: " + deviceToken);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send FCM notification", e);
        }
    }

    private void updateStudentsWithTripId() {
        for (Student student : students) {
            db.collection("students")
                    .document(student.getGrade())
                    .collection("studentList")
                    .document(student.getId())
                    .update("currentTripId", tripId)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update student trip ID: " + student.getId(), e));
        }
    }

    private void clearStudentsTripId() {
        String dropOffTime = formatTimestamp(System.currentTimeMillis());
        for (Student student : students) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentTripId", null);
            updates.put("dropOffTime", dropOffTime);

            db.collection("students")
                    .document(student.getGrade())
                    .collection("studentList")
                    .document(student.getId())
                    .update(updates)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to clear student trip ID: " + student.getId(), e));
        }
    }

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
        }
        return String.format(Locale.getDefault(), "%d mins", minutes);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToastAndFinish(String message) {
        showToast(message);
        finish();
    }
}