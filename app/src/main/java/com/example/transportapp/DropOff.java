package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private boolean locationSharing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop_off);
        initializeFirestore();
        if (!getIntentData()) {
            return;
        }
        initializeViews();
        loadStudentsForVehicle();
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    /** Returns false (and closes the screen) when the driver or vehicle is missing. */
    private boolean getIntentData() {
        Intent intent = getIntent();
        assignedVehicle = intent.getStringExtra("assignedVehicle");
        driverId = intent.getStringExtra("driverId");
        driverName = intent.getStringExtra("driverName");
        assignedRoute = intent.getStringExtra("assignedRoute");

        if (assignedVehicle == null || assignedVehicle.isEmpty()) {
            showToastAndFinish("Vehicle information not found");
            return false;
        }

        if (driverId == null || driverId.isEmpty()) {
            showToastAndFinish("Driver information not found");
            return false;
        }
        return true;
    }

    private void initializeViews() {
        studentListRecyclerView = findViewById(R.id.studentListRecyclerView);
        tripCompleteButton = findViewById(R.id.tripCompleteButton);

        adapter = new StudentStatusAdapter(students, "", false);
        studentListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentListRecyclerView.setAdapter(adapter);

        tripCompleteButton.setOnClickListener(v -> confirmAndCompleteTrip());
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
                        showToast("Could not load the students for this vehicle. Check your connection and try again.");
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
                    BusGroups.syncParents(db, assignedVehicle, students);
                    startLocationSharing();
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

    /** Warns the driver about children left unmarked before the trip is closed. */
    private void confirmAndCompleteTrip() {
        int unmarked = 0;
        for (Student student : students) {
            if (adapter.getStatus(student) == null) unmarked++;
        }
        if (unmarked == 0) {
            completeTrip();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Some children are not marked")
                .setMessage(unmarked + (unmarked == 1 ? " child has" : " children have")
                        + " not been marked as dropped off or not dropped. If you complete the trip now, "
                        + "their parents and the school will be told the drop-off was not confirmed.")
                .setPositiveButton("Complete trip", (d, w) -> completeTrip())
                .setNegativeButton("Go back", null)
                .show();
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

        stopLocationSharing();
        sendDropOffNotificationToParent();
        clearStudentsTripId();
        finish();
    }

    private void sendDropOffNotificationToParent() {
        // Each parent is told what the driver actually recorded for their own child.
        // Writing the notification document triggers the push message (functions/index.js);
        // the old client-side FirebaseMessaging.send() upstream API never reached other phones.
        TripAlerts.Summary summary = TripAlerts.sendTripAlerts(db, students, adapter::getStatus,
                false, tripId, assignedVehicle, driverId, driverName);
        if (summary.skippedNoParentEmail > 0) {
            showToast(summary.skippedNoParentEmail + " student record(s) have no parent email, "
                    + "so those parents were not notified. Ask the school to add it.");
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
        // Only children confirmed as dropped off have their trip cleared (the adapter already
        // stamped their drop-off time). Anyone else keeps currentTripId so the school can see
        // which trip they were last on.
        for (Student student : students) {
            if (!Boolean.TRUE.equals(adapter.getStatus(student))) continue;
            db.collection("students")
                    .document(student.getGrade())
                    .collection("studentList")
                    .document(student.getId())
                    .update("currentTripId", null)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to clear a student's trip ID", e));
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

    // ---------- live bus location ----------

    private final ActivityResultLauncher<String[]> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (TripLocationService.hasLocationPermission(this)) {
                    startLocationSharing();
                } else {
                    showToast("Location is off, so parents won't see the bus on the map for this trip.");
                }
            });

    /** Shares the bus location with parents for as long as this trip runs. */
    private void startLocationSharing() {
        if (tripId == null) return;
        if (!TripLocationService.hasLocationPermission(this)) {
            locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        TripLocationService.start(this, assignedVehicle, tripId, "dropoff");
        locationSharing = true;
    }

    private void stopLocationSharing() {
        if (!locationSharing) return;
        locationSharing = false;
        TripLocationService.stop(this, assignedVehicle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Never keep sharing the location after the trip screen is gone.
        stopLocationSharing();
    }

    private void showToastAndFinish(String message) {
        showToast(message);
        finish();
    }
}