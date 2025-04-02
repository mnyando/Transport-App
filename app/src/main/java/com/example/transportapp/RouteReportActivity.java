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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RouteReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Spinner routeSpinner;
    private TextView avgRouteDurationText, tripsCompletedText, peakTimeText, avgStudentsText;
    private BarChart efficiencyChart;
    private RecyclerView recentTripsRecyclerView;
    private List<Map<String, Object>> routeList = new ArrayList<>();
    private List<Map<String, Object>> recentTrips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_report);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();
        setupBackButton();
        loadRoutes();
        setupChart();
    }

    private void initViews() {
        routeSpinner = findViewById(R.id.routeSpinner);
        avgRouteDurationText = findViewById(R.id.avgRouteDurationText);
        tripsCompletedText = findViewById(R.id.tripsCompletedText);
        peakTimeText = findViewById(R.id.peakTimeText);
        avgStudentsText = findViewById(R.id.avgStudentsText);
        efficiencyChart = findViewById(R.id.efficiencyChart);
        recentTripsRecyclerView = findViewById(R.id.recentTripsRecyclerView);

        // Setup RecyclerView
        recentTripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsRecyclerView.setAdapter(new TripAdapter(recentTrips));
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupChart() {
        efficiencyChart.getDescription().setEnabled(false);
        efficiencyChart.getLegend().setEnabled(false);
        efficiencyChart.getXAxis().setEnabled(false);
        efficiencyChart.setTouchEnabled(false);
        efficiencyChart.setDrawGridBackground(false);
    }

    private void loadRoutes() {
        db.collection("routes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        routeList.clear();
                        List<String> routeNames = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> route = document.getData();
                            route.put("id", document.getId());
                            routeList.add(route);
                            routeNames.add(route.get("name").toString());
                        }

                        // Setup spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, routeNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        routeSpinner.setAdapter(adapter);

                        // Set listener
                        routeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                loadRouteData(routeList.get(position).get("id").toString());
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                });
    }

    private void loadRouteData(String routeId) {
        // Load route stats
        db.collection("trips")
                .whereEqualTo("route", routeId)
                .whereEqualTo("completed", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int tripsCompleted = 0;
                        long totalDuration = 0;
                        int totalStudents = 0;
                        Map<String, Integer> timeDistribution = new HashMap<>();
                        recentTrips.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            tripsCompleted++;
                            Long duration = document.getLong("durationMillis");
                            if (duration != null) {
                                totalDuration += duration;
                            }

                            Long students = document.getLong("studentCount");
                            if (students != null) {
                                totalStudents += students;
                            }

                            // Track time distribution for peak time calculation
                            String timePeriod = getTimePeriod(document.getLong("startTimeMillis"));
                            timeDistribution.put(timePeriod, timeDistribution.getOrDefault(timePeriod, 0) + 1);

                            // Add to recent trips (last 5)
                            if (recentTrips.size() < 5) {
                                recentTrips.add(document.getData());
                            }
                        }

                        // Update UI
                        avgRouteDurationText.setText(formatDuration(totalDuration / Math.max(1, tripsCompleted)));
                        tripsCompletedText.setText(String.valueOf(tripsCompleted));
                        avgStudentsText.setText(String.format(Locale.getDefault(), "%.1f",
                                totalStudents / (float) Math.max(1, tripsCompleted)));

                        // Calculate peak time
                        String peakTime = calculatePeakTime(timeDistribution);
                        peakTimeText.setText(peakTime);

                        // Update chart
                        updateEfficiencyChart(tripsCompleted, totalDuration, totalStudents);

                        // Update RecyclerView
                        recentTripsRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private String getTimePeriod(Long timestamp) {
        // Implement logic to categorize time into periods (e.g., "8-9 AM")
        return "8-9 AM"; // Placeholder
    }

    private String calculatePeakTime(Map<String, Integer> timeDistribution) {
        // Implement logic to find most frequent time period
        return "8:30 AM"; // Placeholder
    }

    private void updateEfficiencyChart(int trips, long totalDuration, int totalStudents) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, trips));
        entries.add(new BarEntry(1, totalDuration / 60000f)); // Convert to minutes
        entries.add(new BarEntry(2, totalStudents / (float) Math.max(1, trips)));

        BarDataSet dataSet = new BarDataSet(entries, "Route Efficiency");
        dataSet.setColors(new int[] {R.color.green, R.color.blue, R.color.gold}, this);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.3f);
        efficiencyChart.setData(barData);
        efficiencyChart.invalidate();
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format(Locale.getDefault(), "%dm", minutes);
    }
}