package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DriverReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Spinner driverSpinner;
    private TextView totalTripsText, avgDurationText, onTimeRateText, studentsTransportedText;
    private RecyclerView recentTripsRecyclerView;
    private List<Map<String, Object>> driverList = new ArrayList<>();
    private List<Map<String, Object>> recentTrips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_report);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();
        setupBackButton();
        loadDrivers();
    }

    private void initViews() {
        driverSpinner = findViewById(R.id.driverSpinner);
        totalTripsText = findViewById(R.id.totalTripsText);
        avgDurationText = findViewById(R.id.avgDurationText);
        onTimeRateText = findViewById(R.id.onTimeRateText);
        studentsTransportedText = findViewById(R.id.studentsTransportedText);
        recentTripsRecyclerView = findViewById(R.id.recentTripsRecyclerView);

        // Setup RecyclerView
        recentTripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsRecyclerView.setAdapter(new TripAdapter(recentTrips));
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadDrivers() {
        db.collection("drivers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        driverList.clear();
                        List<String> driverNames = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> driver = document.getData();
                            driver.put("id", document.getId());
                            driverList.add(driver);
                            driverNames.add(driver.get("name").toString());
                        }

                        // Setup spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, driverNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        driverSpinner.setAdapter(adapter);

                        // Set listener
                        driverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                loadDriverData(driverList.get(position).get("id").toString());
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                });
    }

    private void loadDriverData(String driverId) {
        // Load driver stats
        db.collection("trips")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("completed", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalTrips = 0;
                        long totalDuration = 0;
                        int onTimeTrips = 0;
                        int totalStudents = 0;
                        recentTrips.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            totalTrips++;
                            Long duration = document.getLong("durationMillis");
                            if (duration != null) {
                                totalDuration += duration;
                                // Assuming trips under 30 mins are "on time"
                                if (duration < TimeUnit.MINUTES.toMillis(30)) {
                                    onTimeTrips++;
                                }
                            }

                            Long students = document.getLong("studentCount");
                            if (students != null) {
                                totalStudents += students;
                            }

                            // Add to recent trips (last 5)
                            if (recentTrips.size() < 5) {
                                recentTrips.add(document.getData());
                            }
                        }

                        // Update UI
                        totalTripsText.setText(String.valueOf(totalTrips));
                        avgDurationText.setText(formatDuration(totalDuration / Math.max(1, totalTrips)));
                        onTimeRateText.setText(String.format(Locale.getDefault(), "%d%%",
                                (int) ((onTimeTrips * 100f) / Math.max(1, totalTrips))));
                        studentsTransportedText.setText(String.valueOf(totalStudents));

                        // Update RecyclerView
                        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format(Locale.getDefault(), "%dm", minutes);
    }
}