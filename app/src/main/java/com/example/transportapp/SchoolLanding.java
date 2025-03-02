package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;

public class SchoolLanding extends AppCompatActivity {

    // Declare Firebase Firestore instance
    private FirebaseFirestore db;

    // Declare CardViews for each action
    private CardView addRoute, modifyRoute, deleteRoute;
    private CardView addStaff, modifyStaff, deleteStaff;
    private CardView addStudent, modifyStudent, deleteStudent;
    private CardView addVehicle, modifyVehicle, deleteVehicle;

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
        addStudent = findViewById(R.id.addStudent);
        modifyStudent = findViewById(R.id.modifyStudent);
        deleteStudent = findViewById(R.id.deleteStudent);
        addVehicle = findViewById(R.id.AddVehicle);
        modifyVehicle = findViewById(R.id.ModifyVehicle);
        deleteVehicle = findViewById(R.id.DeleteVehicle);

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

    // Generic function to open activity
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(SchoolLanding.this, activityClass);
        startActivity(intent);
    }
}
