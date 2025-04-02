package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class Reports extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        // Initialize card views
        CardView driverCard = findViewById(R.id.driverCard);
        CardView studentCard = findViewById(R.id.studentCard);
        CardView routeCard = findViewById(R.id.routeCard);

        // Set click listeners
        driverCard.setOnClickListener(v -> {
            Intent intent = new Intent(Reports.this, DriverReportActivity.class);
            startActivity(intent);
        });

        studentCard.setOnClickListener(v -> {
            Intent intent = new Intent(Reports.this, StudentReportActivity.class);
            startActivity(intent);
        });

        routeCard.setOnClickListener(v -> {
            Intent intent = new Intent(Reports.this, RouteReportActivity.class);
            startActivity(intent);
        });
    }
}