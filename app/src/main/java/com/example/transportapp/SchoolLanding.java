package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.AggregateSource;
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

    private Button reportsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_landing);
        Session.setUpHomeToolbar(this, null);

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

        reportsButton = findViewById(R.id.reportsButton);

        refreshLayout = findViewById(R.id.schoolRefresh);
        refreshLayout.setColorSchemeResources(R.color.brand_primary);
        refreshLayout.setOnRefreshListener(this::refreshCounts);

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

        reportsButton.setOnClickListener(v -> openActivity(Reports.class));
        findViewById(R.id.inviteCodeButton).setOnClickListener(v -> InviteCodes.showDialog(this));
    }

    /** Numbers are reloaded each time the school comes back here, e.g. after adding a student. */
    @Override
    protected void onResume() {
        super.onResume();
        refreshCounts();
    }

    private SwipeRefreshLayout refreshLayout;
    private int pendingCounts;

    private void refreshCounts() {
        pendingCounts = 4;
        countTotalStudents();
        countRoutes();
        countVehicles();
        countDrivers();
    }

    private void countDone() {
        if (--pendingCounts <= 0) refreshLayout.setRefreshing(false);
    }

    // Function to open another activity
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(SchoolLanding.this, activityClass);
        startActivity(intent);
    }

    private void countTotalStudents() {
        db.collectionGroup(FirestorePaths.STUDENT_LIST).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> studentsEnrolledText.setText(String.valueOf(result.getCount())))
                .addOnFailureListener(e -> UserErrors.show(this, getString(R.string.count_failed_students), e))
                .addOnCompleteListener(t -> countDone());
    }

    // Count total routes
    private void countRoutes() {
        db.collection(FirestorePaths.ROUTES).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> routesTraveledText.setText(String.valueOf(result.getCount())))
                .addOnFailureListener(e -> UserErrors.show(this, getString(R.string.count_failed_routes), e))
                .addOnCompleteListener(t -> countDone());
    }

    // Count active and inactive vehicles
    private void countVehicles() {
        db.collection(FirestorePaths.VEHICLES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeVehicles = 0;
                    int inactiveVehicles = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if ("Active".equals(status)) { // Exact match for "Active"
                            activeVehicles++;
                        } else if ("Not Active".equals(status)) { // Exact match for "Not Active"
                            inactiveVehicles++;
                        }
                    }

                    activeVehiclesText.setText(String.valueOf(activeVehicles));
                    inactiveVehiclesText.setText(String.valueOf(inactiveVehicles));
                })
                .addOnFailureListener(e -> UserErrors.show(this, getString(R.string.count_failed_vehicles), e))
                .addOnCompleteListener(t -> countDone());
    }

    // Count active and inactive drivers
    private void countDrivers() {
        db.collection(FirestorePaths.STAFF)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeDrivers = 0;
                    int inactiveDrivers = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Staff also holds attendants; only drivers count here.
                        if (!"Driver".equalsIgnoreCase(doc.getString("role"))) continue;
                        String status = doc.getString("status");
                        if ("Active".equals(status)) { // Exact match for "Active"
                            activeDrivers++;
                        } else if ("Not Active".equals(status)) { // Exact match for "Not Active"
                            inactiveDrivers++;
                        }
                    }

                    activeDriversText.setText(String.valueOf(activeDrivers));
                    inactiveDriversText.setText(String.valueOf(inactiveDrivers));
                })
                .addOnFailureListener(e -> UserErrors.show(this, getString(R.string.count_failed_drivers), e))
                .addOnCompleteListener(t -> countDone());
    }
}
