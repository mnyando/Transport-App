package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SchoolLanding extends AppCompatActivity {

    // Declare Firebase Firestore instance
    private FirebaseFirestore db;

    // Declare UI elements
    private CardView addRoute, modifyRoute, deleteRoute;
    private CardView addStaff, modifyStaff, deleteStaff;
    private CardView addStudent, modifyStudent, deleteStudent;
    private CardView addVehicle, modifyVehicle, deleteVehicle;

    private TextView studentsEnrolledText, routesTraveledText, activeVehiclesText, inactiveVehiclesText, activeDriversText, inactiveDriversText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        addRoute = findViewById(R.id.AddRoute);
        modifyRoute = findViewById(R.id.ModifyRoute);
        deleteRoute = findViewById(R.id.DeleteRoute);
        addStaff = findViewById(R.id.AddStaffRecord);
        modifyStaff = findViewById(R.id.ModifyStaffRecord);
        deleteStaff = findViewById(R.id.DeleteStaffRecord);
        addStudent = findViewById(R.id.addStudent);
        modifyStudent = findViewById(R.id.modifyStudent);
        deleteStudent = findViewById(R.id.deleteStudent);
        addVehicle = findViewById(R.id.AddVehicle);
        modifyVehicle = findViewById(R.id.ModifyVehicle);
        deleteVehicle = findViewById(R.id.DeleteVehicle);

        // TextViews for displaying stats
        studentsEnrolledText = findViewById(R.id.studentsEnrolledText);
        routesTraveledText = findViewById(R.id.routesTraveledText);
        activeVehiclesText = findViewById(R.id.activeVehiclesText);
        inactiveVehiclesText = findViewById(R.id.inactiveVehiclesText);
        activeDriversText = findViewById(R.id.activeDriversText);
        inactiveDriversText = findViewById(R.id.inactiveDriversText);

        // Fetch data from Firestore
        countTotalStudents();
        countRoutes();
        countVehicles();
        countDrivers();

        // Set onClickListeners for Route actions
        addRoute.setOnClickListener(v -> openActivity(AddRoute.class));
        modifyRoute.setOnClickListener(v -> openActivity(ModifyRoute.class));
        deleteRoute.setOnClickListener(v -> openActivity(DeleteRoute.class));

        // Set onClickListeners for Staff actions
        addStaff.setOnClickListener(v -> openActivity(AddStaff.class));
        modifyStaff.setOnClickListener(v -> openActivity(ModifyStaff.class));
        deleteStaff.setOnClickListener(v -> openActivity(DeleteStaff.class));

        // Set onClickListeners for Student actions
        addStudent.setOnClickListener(v -> openActivity(AddStudent.class));
        modifyStudent.setOnClickListener(v -> openActivity(ModifyStudent.class));
        deleteStudent.setOnClickListener(v -> openActivity(DeleteStudent.class));

        // Set onClickListeners for Vehicle actions
        addVehicle.setOnClickListener(v -> openActivity(AddVehicle.class));
        modifyVehicle.setOnClickListener(v -> openActivity(ModifyVehicle.class));
        deleteVehicle.setOnClickListener(v -> openActivity(DeleteVehicle.class));
    }

    // Function to open another activity
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(SchoolLanding.this, activityClass);
        startActivity(intent);
    }

    // Count total students
    private void countTotalStudents() {
        db.collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalStudents = queryDocumentSnapshots.size();
                    studentsEnrolledText.setText(String.valueOf(totalStudents));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching student count: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Count total routes
    private void countRoutes() {
        db.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalRoutes = queryDocumentSnapshots.size();
                    routesTraveledText.setText(String.valueOf(totalRoutes));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching routes count: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Count active and inactive vehicles
    private void countVehicles() {
        db.collection("vehicles")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeVehicles = 0;
                    int inactiveVehicles = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if ("Active".equalsIgnoreCase(status)) {
                            activeVehicles++;
                        } else {
                            inactiveVehicles++;
                        }
                    }

                    activeVehiclesText.setText(String.valueOf(activeVehicles));
                    inactiveVehiclesText.setText(String.valueOf(inactiveVehicles));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching vehicles count: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Count active and inactive drivers
    private void countDrivers() {
        db.collection("drivers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeDrivers = 0;
                    int inactiveDrivers = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if ("Active".equalsIgnoreCase(status)) {
                            activeDrivers++;
                        } else {
                            inactiveDrivers++;
                        }
                    }

                    activeDriversText.setText(String.valueOf(activeDrivers));
                    inactiveDriversText.setText(String.valueOf(inactiveDrivers));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching drivers count: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
