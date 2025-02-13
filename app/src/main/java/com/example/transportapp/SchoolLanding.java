package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class SchoolLanding extends AppCompatActivity {

    // Declare Firebase Firestore instance
    private FirebaseFirestore db;

    // Declare CardViews for each action
    private CardView addRoute, modifyRoute, deleteRoute;
    private CardView addStaff, modifyStaff, deleteStaff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Link XML elements to Java code
        addRoute = findViewById(R.id.AddRoute);
        modifyRoute = findViewById(R.id.ModifyRoute);
        deleteRoute = findViewById(R.id.DeleteRoute);
        addStaff = findViewById(R.id.AddStaffRecord);
        modifyStaff = findViewById(R.id.ModifyStaffRecord);
        deleteStaff = findViewById(R.id.DeleteStaffRecord);

        // Set onClickListeners for Route actions
        addRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to add a new route
                addNewRoute();
            }
        });

        modifyRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to modify an existing route
                modifyRouteRecord();
            }
        });

        deleteRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to delete a route record
                deleteRouteRecord();
            }
        });

        // Set onClickListeners for Staff actions
        addStaff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to add new staff
                addNewStaff();
            }
        });

        modifyStaff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to modify staff record
                modifyStaffRecord();
            }
        });

        deleteStaff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logic to delete staff record
                deleteStaffRecord();
            }
        });

        // Fetch and display data from Firestore
        fetchRouteData();
        fetchStaffData();
    }

    // Function to add a new route
    private void addNewRoute() {
        // Example: Start a new activity for adding a route
        Intent intent = new Intent(SchoolLanding.this, AddRoute.class);
        startActivity(intent);
    }

    // Function to modify a route record
    private void modifyRouteRecord() {
        // Retrieve specific route details from Firestore and modify them
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String routeName = document.getString("routeName");
                                String routeId = document.getId(); // Retrieve route ID
                                // Implement modify logic here (e.g., open a modify route screen)
                                Intent intent = new Intent(SchoolLanding.this, ModifyRoute.class);
                                intent.putExtra("routeId", routeId);  // Pass the route ID to modify screen
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching routes", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to delete a route record
    private void deleteRouteRecord() {
        // Retrieve and delete a specific route from Firestore
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String routeId = document.getId(); // Retrieve the route ID
                                // Delete the route using its ID
                                db.collection("routes").document(routeId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(SchoolLanding.this, "Route Deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(SchoolLanding.this, "Error deleting route", Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching routes", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to add new staff
    private void addNewStaff() {
        // Start a new activity for adding staff
        Intent intent = new Intent(SchoolLanding.this, AddStaff.class);
        startActivity(intent);
    }

    // Function to modify a staff record
    private void modifyStaffRecord() {
        // Retrieve specific staff details from Firestore and modify them
        db.collection("staff")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String staffName = document.getString("staffName");
                                String staffId = document.getId(); // Retrieve staff ID
                                // Implement modify logic here (e.g., open a modify staff screen)
                                Intent intent = new Intent(SchoolLanding.this, ModifyStaff.class);
                                intent.putExtra("staffId", staffId);  // Pass the staff ID to modify screen
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching staff", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to delete a staff record
    private void deleteStaffRecord() {
        // Retrieve and delete a specific staff from Firestore
        db.collection("staff")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String staffId = document.getId(); // Retrieve the staff ID
                                // Delete the staff using their ID
                                db.collection("staff").document(staffId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(SchoolLanding.this, "Staff Deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(SchoolLanding.this, "Error deleting staff", Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching staff", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to fetch route data from Firestore
    private void fetchRouteData() {
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String routeName = document.getString("routeName");
                                String routeDetails = document.getString("routeDetails"); // Fetch additional route details
                                // Example of displaying route data (you can update the UI with this info)
                                Toast.makeText(SchoolLanding.this, "Route: " + routeName + "\nDetails: " + routeDetails, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching route data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to fetch staff data from Firestore
    private void fetchStaffData() {
        db.collection("staff")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String staffName = document.getString("staffName");
                                String staffRole = document.getString("staffRole"); // Fetch additional staff details
                                // Example of displaying staff data (you can update the UI with this info)
                                Toast.makeText(SchoolLanding.this, "Staff: " + staffName + "\nRole: " + staffRole, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SchoolLanding.this, "Error fetching staff data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
