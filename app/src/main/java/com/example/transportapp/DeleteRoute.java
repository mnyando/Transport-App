package com.example.transportapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteRoute extends AppCompatActivity {

    private EditText routeNameInput, totalDistanceInput, routeDescriptionInput;
    private Spinner vehicleDropdown;
    private Button deleteRouteButton;
    private RecyclerView routeRecyclerView;

    private FirebaseFirestore firestore;
    private List<Route> routeList;
    private RouteAdapter routeAdapter;
    private String selectedRouteId;
    private String selectedRouteName;
    private List<String> vehicleNamesList;
    private ArrayAdapter<String> vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_route);

        // Initialize UI elements
        routeNameInput = findViewById(R.id.routeNameInput);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        deleteRouteButton = findViewById(R.id.deleteRouteButton);
        routeRecyclerView = findViewById(R.id.routeRecyclerView);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        routeList = new ArrayList<>();
        routeAdapter = new RouteAdapter(routeList, this::onRouteSelected);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeRecyclerView.setAdapter(routeAdapter);

        // Initialize Spinner
        vehicleNamesList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleNamesList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);

        // Fetch data
        fetchVehicles();
        fetchRoutes();

        // This screen only shows the details; edits are made on Modify Route.
        routeNameInput.setEnabled(false);
        totalDistanceInput.setEnabled(false);
        routeDescriptionInput.setEnabled(false);
        vehicleDropdown.setEnabled(false);

        // Set button click listener
        deleteRouteButton.setOnClickListener(v -> showDeleteConfirmationDialog()); // Updated to show dialog
    }

    private void fetchVehicles() {
        firestore.collection(FirestorePaths.VEHICLES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleNamesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String vehicleName = document.getString("vehicleName");
                        if (vehicleName != null) {
                            vehicleNamesList.add(vehicleName);
                        }
                    }
                    vehicleAdapter.notifyDataSetChanged();
                    if (vehicleNamesList.isEmpty()) {
                        Toast.makeText(this, "No vehicles found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    UserErrors.show(this, "Could not load vehicles", e);
                });
    }

    private void fetchRoutes() {
        firestore.collection(FirestorePaths.ROUTES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Route route = document.toObject(Route.class);
                        route.setId(document.getId());
                        routeList.add(route);
                    }
                    routeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> UserErrors.show(this, "Could not load routes", e));
    }

    private void onRouteSelected(Route route) {
        selectedRouteId = route.getId();
        selectedRouteName = route.getRouteName();
        routeNameInput.setText(route.getRouteName());
        totalDistanceInput.setText(String.valueOf(route.getTotalDistance()));
        routeDescriptionInput.setText(route.getRouteDescription());

        int position = vehicleAdapter.getPosition(route.getVehicle());
        if (position >= 0) {
            vehicleDropdown.setSelection(position);
        } else {
            vehicleDropdown.setSelection(0);
        }
    }

    private void showDeleteConfirmationDialog() {
        if (selectedRouteId == null) {
            Toast.makeText(this, "Please select a route to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Students and staff point at the route by name; removing it would strand them.
        deleteRouteButton.setEnabled(false);
        String name = selectedRouteName != null ? selectedRouteName : "";
        Task<Long> students = RecordLinks.studentsOnRoute(firestore, name);
        Task<Long> staff = RecordLinks.staffWith(firestore, "assignedRoute", name);
        Tasks.whenAllComplete(students, staff).addOnCompleteListener(t -> {
            deleteRouteButton.setEnabled(true);
            if (!students.isSuccessful() || !staff.isSuccessful()) {
                UserErrors.show(this, getString(R.string.checking_links_failed),
                        !students.isSuccessful() ? students.getException() : staff.getException());
                return;
            }
            long children = students.getResult();
            long crew = staff.getResult();
            if (children > 0 || crew > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Route Record")
                        .setMessage(getString(R.string.error_route_delete_in_use, (int) children, (int) crew))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete Route Record")
                    .setMessage("Are you sure you want to delete this route?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteRouteFromFirestore())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteRouteFromFirestore() {
        if (selectedRouteId == null) return;
        deleteRouteButton.setEnabled(false);
        firestore.collection(FirestorePaths.ROUTES)
                .document(selectedRouteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    deleteRouteButton.setEnabled(true);
                    Toast.makeText(this, "Route deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchRoutes(); // Refresh list after deletion
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    deleteRouteButton.setEnabled(true);
                    UserErrors.show(this, "Could not delete route", e);
                });
    }

    private void clearFields() {
        selectedRouteId = null;
        selectedRouteName = null;
        routeNameInput.setText("");
        totalDistanceInput.setText("");
        routeDescriptionInput.setText("");
        vehicleDropdown.setSelection(0);
    }
}