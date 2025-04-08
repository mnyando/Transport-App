package com.example.transportapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DriverReportActivity extends AppCompatActivity {
    private static final String TAG = "DriverReportActivity";
    private FirebaseFirestore db;
    private TextView totalTripsText, avgDurationText, onTimeRateText, studentsTransportedText;
    private TextView driverNameText, driverRouteText;
    private RecyclerView recentTripsRecyclerView;
    private EditText searchEditText;
    private List<Map<String, Object>> driverList = new ArrayList<>();
    private List<Map<String, Object>> recentTrips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_report);
        Log.d(TAG, "Activity created");

        db = FirebaseFirestore.getInstance();
        initViews();
        setupBackButton();
        setupSearch();
        clearAllData(); // Clear all data by default
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");
        totalTripsText = findViewById(R.id.totalTripsText);
        avgDurationText = findViewById(R.id.avgDurationText);
        onTimeRateText = findViewById(R.id.onTimeRateText);
        studentsTransportedText = findViewById(R.id.studentsTransportedText);
        recentTripsRecyclerView = findViewById(R.id.recentTripsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);

        driverNameText = findViewById(R.id.driverNameText);
        driverRouteText = findViewById(R.id.driverRouteText);

        recentTripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsRecyclerView.setAdapter(new TripAdapter(recentTrips));
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void setupSearch() {
        Log.d(TAG, "Setting up search");
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
                                Log.d(TAG, "Performing search for: " + s);
                                performDriverSearch(s.toString());
                            } else if (s.length() == 0) {
                                Log.d(TAG, "Search cleared");
                                clearAllData(); // Clear all data when search is empty
                            }
                        });
                    }
                }, DELAY);
            }
        });
    }

    private void performDriverSearch(String query) {
        Log.d(TAG, "Searching drivers with query: " + query);
        db.collection("staff")
                .orderBy("staffName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Driver search successful, found " + task.getResult().size() + " results");
                        driverList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> driver = document.getData();
                            driver.put("id", document.getId());
                            driverList.add(driver);
                        }

                        if (!driverList.isEmpty()) {
                            String driverId = driverList.get(0).get("id").toString();
                            Log.d(TAG, "Auto-selecting first driver in search results: " + driverId);
                            updateDriverInfoCard(driverList.get(0));
                            loadDriverData(driverId);
                        } else {
                            Log.d(TAG, "No drivers found in search");
                            clearAllData();
                        }
                    } else {
                        Log.e(TAG, "Driver search failed", task.getException());
                        clearAllData();
                    }
                });
    }

    private void loadDriverData(String driverId) {
        Log.d(TAG, "Loading data for driver: " + driverId);
        db.collection("trips")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("completed", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Loaded " + task.getResult().size() + " trips for driver");
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
                                if (duration < TimeUnit.MINUTES.toMillis(30)) {
                                    onTimeTrips++;
                                }
                            }

                            Long students = document.getLong("studentCount");
                            if (students != null) {
                                totalStudents += students;
                            }

                            if (recentTrips.size() < 5) {
                                recentTrips.add(document.getData());
                            }
                        }

                        totalTripsText.setText(String.valueOf(totalTrips));
                        avgDurationText.setText(formatDuration(totalDuration / Math.max(1, totalTrips)));
                        onTimeRateText.setText(String.format(Locale.getDefault(), "%d%%",
                                (int) ((onTimeTrips * 100f) / Math.max(1, totalTrips))));
                        studentsTransportedText.setText(String.valueOf(totalStudents));

                        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error loading driver data", task.getException());
                    }
                });
    }

    private void updateDriverInfoCard(Map<String, Object> driver) {
        Log.d(TAG, "Updating driver info card");
        if (driver != null) {
            String name = (String) driver.get("staffName");
            String route = (String) driver.get("assignedRoute");

            driverNameText.setText(name != null ? name : "Unknown Driver");
            driverRouteText.setText(route != null ? route : "No route assigned");
        }
    }

    private void clearAllData() {
        Log.d(TAG, "Clearing all data");
        // Clear stats
        totalTripsText.setText("");
        avgDurationText.setText("");
        onTimeRateText.setText("");
        studentsTransportedText.setText("");

        // Clear driver info
        driverNameText.setText("");
        driverRouteText.setText("");

        // Clear recent trips
        recentTrips.clear();
        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format(Locale.getDefault(), "%dm", minutes);
    }
}