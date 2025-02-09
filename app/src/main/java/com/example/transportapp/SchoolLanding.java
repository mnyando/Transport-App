package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

public class SchoolLanding extends AppCompatActivity {

    private ImageButton backButton;
    private TextView toolbarTitle, studentsEnrolled, routesTravelled, activeVehicles, inactiveVehicles, activeDrivers, inactiveDrivers;
    private ImageView landingImage, enrolledIcon, routesIcon, activeVehiclesIcon, inactiveVehiclesIcon, activeDriversIcon, inactiveDriversIcon;
    private GridLayout generalInfoGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_landing);  // Replace with the correct XML file name

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.top_navigation_bar);
        setSupportActionBar(toolbar);

        // Initialize the UI components
        backButton = findViewById(R.id.back_button);
        toolbarTitle = findViewById(R.id.toolbar_title);
        landingImage = findViewById(R.id.landing_image);
        studentsEnrolled = findViewById(R.id.students_enrolled_text); // Assuming a corresponding ID
        routesTravelled = findViewById(R.id.routes_travelled_text); // Assuming a corresponding ID
        activeVehicles = findViewById(R.id.active_vehicles_text); // Assuming a corresponding ID
        inactiveVehicles = findViewById(R.id.inactive_vehicles_text); // Assuming a corresponding ID
        activeDrivers = findViewById(R.id.active_drivers_text); // Assuming a corresponding ID
        inactiveDrivers = findViewById(R.id.inactive_drivers_text); // Assuming a corresponding ID

        // ImageViews
        enrolledIcon = findViewById(R.id.enrolled_icon); // Assuming ID for ImageView
        routesIcon = findViewById(R.id.routes_icon); // Assuming ID for ImageView
        activeVehiclesIcon = findViewById(R.id.active_vehicles_icon); // Assuming ID for ImageView
        inactiveVehiclesIcon = findViewById(R.id.inactive_vehicles_icon); // Assuming ID for ImageView
        activeDriversIcon = findViewById(R.id.active_drivers_icon); // Assuming ID for ImageView
        inactiveDriversIcon = findViewById(R.id.inactive_drivers_icon); // Assuming ID for ImageView

        // Grid Layout for general information
        generalInfoGrid = findViewById(R.id.general_info_grid); // Assuming ID for GridLayout

        // Set up back button functionality
        backButton.setOnClickListener(v -> finish());  // Navigates back to the previous screen
    }
}
