package com.example.transportapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextInputEditText searchEditText;
    private TextView studentNameText, studentGradeText, studentVehicleText;
    private TextView pickupTimeText, dropoffTimeText;
    private TextView presentCountText, absentCountText;
    private RecyclerView recentTripsRecyclerView;
    private TextView presentPercentText, absentPercentText;
    private List<Map<String, Object>> recentTrips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_report);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();
        setupBackButton();
        setupSearch();
        loadSampleStudentData(); // Replace with your actual data loading
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

        // Setup RecyclerView
        recentTripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsRecyclerView.setAdapter(new TripAdapter(recentTrips));
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Implement search functionality
                if (s.length() > 2) {
                    searchStudents(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchStudents(String query) {
        // Implement your Firestore search logic here
        // Example:
        db.collectionGroup("studentList")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        displayStudentData(document.getData());
                    }
                });
    }

    private void displayStudentData(Map<String, Object> studentData) {
        studentNameText.setText(studentData.get("name").toString());
        studentGradeText.setText("Grade " + studentData.get("grade"));
        studentVehicleText.setText("Bus " + studentData.get("vehicle"));

        // Update attendance stats
        updateAttendanceStats(studentData);

        // Load recent trips
        loadRecentTrips(studentData.get("id").toString());
    }

    private void updateAttendanceStats(Map<String, Object> studentData) {
        // Implement your attendance calculation logic
        int presentDays = 20; // Example - get from studentData
        int absentDays = 5;   // Example - get from studentData
        int totalDays = presentDays + absentDays;

        presentCountText.setText(String.valueOf(presentDays));
        absentCountText.setText(String.valueOf(absentDays));

        if (totalDays > 0) {
            presentPercentText.setText(String.format("%d%%", (presentDays * 100) / totalDays));
            absentPercentText.setText(String.format("%d%%", (absentDays * 100) / totalDays));
        }
    }

    private void loadRecentTrips(String studentId) {
        db.collection("trips")
                .whereArrayContains("studentIds", studentId)
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentTrips.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            recentTrips.add(document.getData());
                        }
                        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    // Sample data loader - replace with your actual implementation
    private void loadSampleStudentData() {
        Map<String, Object> sampleData = new HashMap<>();
        sampleData.put("name", "John Doe");
        sampleData.put("grade", "5");
        sampleData.put("vehicle", "A12");
        sampleData.put("id", "student123");
        sampleData.put("pickupTime", "7:30 AM");
        sampleData.put("dropoffTime", "3:15 PM");

        displayStudentData(sampleData);
    }
}