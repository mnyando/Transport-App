package com.example.transportapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StudentReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextInputEditText searchEditText;
    private TextView studentNameText, studentGradeText, studentVehicleText;
    private TextView pickupTimeText, dropoffTimeText;
    private TextView presentCountText, absentCountText;
    private TextView presentPercentText, absentPercentText;
    private RecyclerView recentTripsRecyclerView;

    private List<Map<String, Object>> recentTrips = new ArrayList<>();
    private String currentStudentId;
    private Map<String, Object> currentStudentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_report);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupBackButton();
        setupSearch();

        // Check if launched with specific student
        currentStudentId = getIntent().getStringExtra("studentId");
        if (currentStudentId != null && !currentStudentId.isEmpty()) {
            fetchStudentData(currentStudentId);
        }
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        studentNameText = findViewById(R.id.studentNameText);
        studentGradeText = findViewById(R.id.studentGradeText);
        studentVehicleText = findViewById(R.id.studentVehicleText);
        pickupTimeText = findViewById(R.id.pickupTimeText);
        dropoffTimeText = findViewById(R.id.dropoffTimeText);
        presentCountText = findViewById(R.id.presentCountText);
        absentCountText = findViewById(R.id.absentCountText);
        presentPercentText = findViewById(R.id.presentPercentText);
        absentPercentText = findViewById(R.id.absentPercentText);
        recentTripsRecyclerView = findViewById(R.id.recentTripsRecyclerView);

        recentTripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsRecyclerView.setAdapter(new TripAdapter(recentTrips));
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 500;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            if (s.length() >= 3) {
                                performSearch();
                            } else if (s.length() == 0) {
                                clearStudentData();
                            }
                        });
                    }
                }, DELAY);
            }
        });
    }

    private void performSearch() {
        final String query = searchEditText.getText().toString().trim();
        Log.d("StudentSearch", "Initiating search for: " + query);

        if (query.length() < 3) {
            showToast("Enter at least 3 characters");
            return;
        }

        db.collectionGroup("studentList")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        if (!documents.isEmpty()) {
                            DocumentSnapshot document = documents.get(0);
                            currentStudentId = document.getString("studentId");
                            Log.d("StudentSearch", "Selected student ID: " + currentStudentId);
                            displayStudentData(document.getData());
                        } else {
                            showToast("No student found");
                            clearStudentData();
                        }
                    } else {
                        Log.e("StudentSearch", "Search failed", task.getException());
                        showToast("Search failed");
                        clearStudentData();
                    }
                });
    }

    private void fetchStudentData(String studentId) {
        db.collectionGroup("studentList")
                .whereEqualTo("studentId", studentId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot studentDoc = task.getResult().getDocuments().get(0);
                        Map<String, Object> studentData = studentDoc.getData();

                        if (studentData != null) {
                            // Get grade from document path or data
                            if (!studentData.containsKey("grade")) {
                                String[] pathParts = studentDoc.getReference().getPath().split("/");
                                if (pathParts.length >= 2) {
                                    studentData.put("grade", pathParts[1]);
                                } else {
                                    studentData.put("grade", "N/A");
                                }
                            }
                            displayStudentData(studentData);
                        }
                    } else {
                        showToast("Student not found");
                    }
                });
    }

    private void displayStudentData(Map<String, Object> studentData) {
        try {
            currentStudentData = studentData;

            // Set basic student info
            studentNameText.setText(studentData.containsKey("name") ?
                    studentData.get("name").toString() : "Unknown");
            studentGradeText.setText("Grade " + (studentData.containsKey("grade") ?
                    studentData.get("grade").toString() : "N/A"));
            studentVehicleText.setText(studentData.containsKey("vehicle") ?
                    studentData.get("vehicle").toString() : "Not assigned");

            // Set pickup/dropoff times
            pickupTimeText.setText(studentData.containsKey("pickupTime") ?
                    studentData.get("pickupTime").toString() : "Not available");
            dropoffTimeText.setText(studentData.containsKey("dropOffTime") ?
                    studentData.get("dropOffTime").toString() : "Not available");

            // Load attendance and trips
            String studentId = studentData.containsKey("studentId") ?
                    studentData.get("studentId").toString() : null;
            if (studentId != null && !studentId.isEmpty()) {
                calculateAttendanceFromTrips(studentId);
                loadRecentTrips(studentId);
            } else {
                updateAttendanceStats(0, 0);
                recentTrips.clear();
                recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e("DisplayData", "Error displaying student data", e);
            showToast("Error displaying data");
            clearStudentData();
        }
    }

    private void calculateAttendanceFromTrips(String studentId) {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        Date startOfMonth = getStartOfMonth();
        Date endOfMonth = getEndOfMonth();

        db.collection("trips")
                .whereEqualTo("completed", true)
                .whereGreaterThanOrEqualTo("endTimeMillis", startOfMonth.getTime())
                .whereLessThanOrEqualTo("endTimeMillis", endOfMonth.getTime())
                .whereArrayContains("studentIds", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int presentDays = task.getResult().size();
                        int schoolDays = getSchoolDaysInMonth();
                        int absentDays = Math.max(0, schoolDays - presentDays);

                        updateAttendanceStats(presentDays, absentDays);

                        if (presentDays > 0) {
                            saveCalculatedAttendance(studentId, presentDays, absentDays, currentMonth);
                        }
                    } else {
                        Log.e("Attendance", "Error calculating attendance", task.getException());
                        updateAttendanceStats(0, 0);
                    }
                });
    }

    private void loadRecentTrips(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            Log.e("Trips", "Invalid student ID provided");
            return;
        }

        Log.d("TripsDebug", "Loading trips for student: " + studentId);

        // First approach: Try to get trips where studentId is in studentIds array
        db.collection("trips")
                .whereArrayContains("studentIds", studentId)
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handleTripQueryResults(task, studentId);
                    } else {
                        // Fallback: Try to get trips by ID from student's pickup/dropoff status
                        loadTripsFromStudentReferences(studentId);
                    }
                });
    }

    private void handleTripQueryResults(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task, String studentId) {
        recentTrips.clear();
        int tripCount = task.getResult().size();
        Log.d("TripsDebug", "Found " + tripCount + " trips via array query");

        if (tripCount > 0) {
            for (QueryDocumentSnapshot document : task.getResult()) {
                processTripDocument(document);
            }
        } else {
            // If no trips found via array query, try reference approach
            loadTripsFromStudentReferences(studentId);
            return;
        }

        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void loadTripsFromStudentReferences(String studentId) {
        if (currentStudentData == null) {
            showToast("No trip data available");
            return;
        }

        List<String> tripIds = new ArrayList<>();

        // Get trip IDs from pickupStatus
        if (currentStudentData.containsKey("pickupStatus")) {
            try {
                Map<String, Object> pickupStatus = (Map<String, Object>) currentStudentData.get("pickupStatus");
                if (pickupStatus != null && pickupStatus.containsKey("tripId")) {
                    tripIds.add(pickupStatus.get("tripId").toString());
                }
            } catch (ClassCastException e) {
                Log.e("Trips", "Error parsing pickupStatus", e);
            }
        }

        // Get trip IDs from dropoffStatus
        if (currentStudentData.containsKey("dropoffStatus")) {
            try {
                Map<String, Object> dropoffStatus = (Map<String, Object>) currentStudentData.get("dropoffStatus");
                if (dropoffStatus != null && dropoffStatus.containsKey("tripId")) {
                    tripIds.add(dropoffStatus.get("tripId").toString());
                }
            } catch (ClassCastException e) {
                Log.e("Trips", "Error parsing dropoffStatus", e);
            }
        }

        if (tripIds.isEmpty()) {
            Log.d("TripsDebug", "No trip references found in student data");
            showToast("No recent trips found");
            return;
        }

        // Fetch trips by their IDs
        db.collection("trips")
                .whereIn(FieldPath.documentId(), tripIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentTrips.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            processTripDocument(document);
                        }
                        if (recentTrips.isEmpty()) {
                            showToast("No trip details found");
                        }
                        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
                    } else {
                        Log.e("Trips", "Error loading trips by ID", task.getException());
                        showToast("Failed to load trip details");
                    }
                });
    }

    private void processTripDocument(QueryDocumentSnapshot document) {
        try {
            Map<String, Object> tripData = document.getData();
            if (tripData != null) {
                tripData.put("id", document.getId());

                // START OF NEW CODE - Trip type identification
                String tripType = "trip"; // default value
                if (currentStudentData != null) {
                    // Check if this is a pickup trip
                    if (currentStudentData.containsKey("pickupStatus")) {
                        Map<String, Object> pickupStatus = (Map<String, Object>) currentStudentData.get("pickupStatus");
                        if (pickupStatus != null &&
                                document.getId().equals(pickupStatus.get("tripId"))) {
                            tripType = "pickup";
                        }
                    }

                    // Check if this is a dropoff trip
                    if (currentStudentData.containsKey("dropoffStatus")) {
                        Map<String, Object> dropoffStatus = (Map<String, Object>) currentStudentData.get("dropoffStatus");
                        if (dropoffStatus != null &&
                                document.getId().equals(dropoffStatus.get("tripId"))) {
                            tripType = "dropoff";
                        }
                    }
                }
                tripData.put("tripType", tripType);
                // END OF NEW CODE

                // Format duration if timestamps available
                if (tripData.containsKey("startTimeMillis") &&
                        tripData.containsKey("endTimeMillis")) {
                    try {
                        long start = ((Number)tripData.get("startTimeMillis")).longValue();
                        long end = ((Number)tripData.get("endTimeMillis")).longValue();
                        tripData.put("durationFormatted", formatDuration(start, end));
                    } catch (Exception e) {
                        Log.e("Trips", "Error formatting duration", e);
                        tripData.put("durationFormatted", "N/A");
                    }
                }
                recentTrips.add(tripData);
            }
        } catch (Exception e) {
            Log.e("Trips", "Error processing trip document", e);
        }
    }
    // Helper methods remain the same as in your original code
    private Date getStartOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private int getSchoolDaysInMonth() {
        return 20;
    }

    private void saveCalculatedAttendance(String studentId, int presentDays, int absentDays, String month) {
        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("studentId", studentId);
        attendanceRecord.put("presentDays", presentDays);
        attendanceRecord.put("absentDays", absentDays);
        attendanceRecord.put("calculatedFromTrips", true);
        attendanceRecord.put("timestamp", FieldValue.serverTimestamp());

        db.collection("attendance")
                .document(month)
                .collection("records")
                .add(attendanceRecord)
                .addOnFailureListener(e -> {
                    Log.e("Attendance", "Error saving attendance", e);
                });
    }

    private void updateAttendanceStats(int presentDays, int absentDays) {
        presentCountText.setText(String.valueOf(presentDays));
        absentCountText.setText(String.valueOf(absentDays));

        int totalDays = presentDays + absentDays;
        if (totalDays > 0) {
            presentPercentText.setText(String.format(Locale.getDefault(),
                    "%d%%", (presentDays * 100) / totalDays));
            absentPercentText.setText(String.format(Locale.getDefault(),
                    "%d%%", (absentDays * 100) / totalDays));
        } else {
            presentPercentText.setText("0%");
            absentPercentText.setText("0%");
        }
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

    private void clearStudentData() {
        studentNameText.setText("");
        studentGradeText.setText("");
        studentVehicleText.setText("");
        pickupTimeText.setText("");
        dropoffTimeText.setText("");
        presentCountText.setText("0");
        absentCountText.setText("0");
        presentPercentText.setText("0%");
        absentPercentText.setText("0%");
        recentTrips.clear();
        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
        currentStudentData = null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}