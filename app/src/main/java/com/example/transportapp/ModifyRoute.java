package com.example.transportapp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyRoute extends AppCompatActivity {

    private EditText routeNameInput, totalDistanceInput, routeDescriptionInput;
    private EditText pickupTimeInput, dropOffTimeInput; // New fields
    private Spinner vehicleDropdown;
    private Button updateRouteButton;
    private RecyclerView routeRecyclerView;

    private FirebaseFirestore firestore;
    private List<Route> routeList;
    private RouteAdapter routeAdapter;
    private String selectedRouteId;
    /** The route as loaded, so a save can tell what changed and move its students with it. */
    private Route selectedRoute;
    private List<String> vehicleNamesList;
    private ArrayAdapter<String> vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_route);

        // Initialize UI elements
        routeNameInput = findViewById(R.id.routeNameInput);
        totalDistanceInput = findViewById(R.id.totalDistanceInput);
        routeDescriptionInput = findViewById(R.id.routeDescriptionInput);
        pickupTimeInput = findViewById(R.id.pickupTimeInput); // Initialize pickup time
        dropOffTimeInput = findViewById(R.id.dropOffTimeInput); // Initialize drop-off time
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        updateRouteButton = findViewById(R.id.updateRouteButton);
        routeRecyclerView = findViewById(R.id.routeRecyclerView);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        routeList = new ArrayList<>();
        routeAdapter = new RouteAdapter(routeList, this::onRouteSelected);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeRecyclerView.setAdapter(routeAdapter);

        // Initialize Spinner with vehicle names
        vehicleNamesList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleNamesList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);

        // Fetch data
        fetchVehicles();
        fetchRoutes();

        // Set button click listener for updating route
        updateRouteButton.setOnClickListener(v -> updateRouteInFirestore());

        // Set click listeners for time inputs to show TimePickerDialog
        pickupTimeInput.setOnClickListener(v -> showTimePickerDialog(pickupTimeInput));
        dropOffTimeInput.setOnClickListener(v -> showTimePickerDialog(dropOffTimeInput));
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
                .addOnFailureListener(e -> UserErrors.show(this, "Could not load vehicles", e));
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
        selectedRoute = route;
        routeNameInput.setText(route.getRouteName());
        totalDistanceInput.setText(String.valueOf(route.getTotalDistance()));
        routeDescriptionInput.setText(route.getRouteDescription());
        pickupTimeInput.setText(route.getPickupTime()); // Set pickup time from database
        dropOffTimeInput.setText(route.getDropOffTime()); // Set drop-off time from database

        // Set the Spinner to the route's vehicle
        int position = vehicleAdapter.getPosition(route.getVehicle());
        if (position >= 0) {
            vehicleDropdown.setSelection(position);
        } else {
            vehicleDropdown.setSelection(0); // Default to first item if not found
        }
    }

    private void updateRouteInFirestore() {
        if (selectedRouteId == null) {
            Toast.makeText(this, "Please select a route to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String routeName = routeNameInput.getText().toString().trim();
        String totalDistanceStr = totalDistanceInput.getText().toString().trim();
        String routeDescription = routeDescriptionInput.getText().toString().trim();
        String selectedVehicle = vehicleDropdown.getSelectedItem() != null ?
                vehicleDropdown.getSelectedItem().toString() : "";
        String pickupTime = pickupTimeInput.getText().toString().trim(); // Get pickup time
        String dropOffTime = dropOffTimeInput.getText().toString().trim(); // Get drop-off time

        if (routeName.isEmpty() || totalDistanceStr.isEmpty() || routeDescription.isEmpty() ||
                selectedVehicle.isEmpty() || pickupTime.isEmpty() || dropOffTime.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalDistance;
        try {
            totalDistance = Double.parseDouble(totalDistanceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalDistance < 0) {
            Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!RouteTimes.dropOffAfterPickup(pickupTime, dropOffTime)) {
            Toast.makeText(this, R.string.error_dropoff_before_pickup, Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare updated data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("routeName", routeName);
        updatedData.put("totalDistance", totalDistance);
        updatedData.put("routeDescription", routeDescription);
        updatedData.put("vehicle", selectedVehicle);
        updatedData.put("pickupTime", pickupTime); // Include pickup time
        updatedData.put("dropOffTime", dropOffTime); // Include drop-off time

        String routeId = selectedRouteId;
        String oldName = selectedRoute != null ? selectedRoute.getRouteName() : routeName;
        String oldVehicle = selectedRoute != null ? selectedRoute.getVehicle() : selectedVehicle;
        boolean vehicleChanged = oldVehicle == null || !oldVehicle.equals(selectedVehicle);
        boolean studentsNeedUpdate = selectedRoute == null
                || !routeName.equals(oldName) || vehicleChanged
                || !pickupTime.equals(selectedRoute.getPickupTime())
                || !dropOffTime.equals(selectedRoute.getDropOffTime());

        updateRouteButton.setEnabled(false);

        // 1. Route names must stay unique: students and staff find their route by name.
        Task<Boolean> nameCheck = RecordLinks.nameTaken(firestore, FirestorePaths.ROUTES,
                "routeName", routeName, routeId);
        // 2. A new bus must have room for every child already on this route.
        Task<Long> onRoute = vehicleChanged && oldName != null
                ? RecordLinks.studentsOnRoute(firestore, oldName) : Tasks.forResult(0L);
        Task<com.google.firebase.firestore.QuerySnapshot> newBus = vehicleChanged
                ? firestore.collection(FirestorePaths.VEHICLES)
                        .whereEqualTo("vehicleName", selectedVehicle).limit(1).get()
                : Tasks.forResult(null);

        Tasks.whenAllComplete(nameCheck, onRoute, newBus).addOnCompleteListener(all -> {
            if (!nameCheck.isSuccessful() || !onRoute.isSuccessful() || !newBus.isSuccessful()) {
                updateRouteButton.setEnabled(true);
                Exception e = !nameCheck.isSuccessful() ? nameCheck.getException()
                        : !onRoute.isSuccessful() ? onRoute.getException() : newBus.getException();
                UserErrors.show(this, getString(R.string.checking_links_failed), e);
                return;
            }
            if (Boolean.TRUE.equals(nameCheck.getResult())) {
                updateRouteButton.setEnabled(true);
                routeNameInput.setError(getString(R.string.error_route_name_taken, routeName));
                routeNameInput.requestFocus();
                return;
            }
            long children = onRoute.getResult() != null ? onRoute.getResult() : 0L;
            if (vehicleChanged && children > 0 && newBus.getResult() != null
                    && !newBus.getResult().isEmpty()) {
                DocumentSnapshot bus = newBus.getResult().getDocuments().get(0);
                int capacity = Vehicle.parseCapacity(bus.get("capacity"));
                Long stored = bus.getLong("studentCount");
                long used = stored != null ? stored : 0L;
                long free = capacity - used;
                if (capacity > 0 && children > free) {
                    updateRouteButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_bus_too_small_for_route,
                            selectedVehicle, (int) Math.max(0, free), (int) children),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            saveRoute(routeId, updatedData, oldName, routeName, selectedVehicle,
                    pickupTime, dropOffTime, studentsNeedUpdate);
        });
    }

    private void saveRoute(String routeId, Map<String, Object> updatedData, String oldName,
                           String newName, String vehicle, String pickupTime, String dropOffTime,
                           boolean studentsNeedUpdate) {
        firestore.collection(FirestorePaths.ROUTES)
                .document(routeId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    if (!studentsNeedUpdate || oldName == null) {
                        finishUpdate(getResources().getQuantityString(R.plurals.route_updated_students, 0, 0));
                        return;
                    }
                    // Students keep their own copy of route, bus and times; move them with it.
                    RecordLinks.applyRouteChange(firestore, oldName, newName, vehicle,
                                    pickupTime, dropOffTime)
                            .addOnSuccessListener(parentEmails -> {
                                // Parents need access to the new bus's location and chat.
                                for (String email : parentEmails) {
                                    BusGroups.addParent(firestore, vehicle, email);
                                }
                                finishUpdate(getResources().getQuantityString(R.plurals.route_updated_students,
                                        parentEmails.size(), parentEmails.size()));
                            })
                            .addOnFailureListener(e -> {
                                updateRouteButton.setEnabled(true);
                                UserErrors.show(this, getString(R.string.route_update_partial), e);
                                fetchRoutes();
                            });
                })
                .addOnFailureListener(e -> {
                    updateRouteButton.setEnabled(true);
                    UserErrors.show(this, "Could not update route", e);
                });
    }

    private void finishUpdate(String message) {
        updateRouteButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        selectedRouteId = null;
        selectedRoute = null;
        routeNameInput.setText("");
        totalDistanceInput.setText("");
        routeDescriptionInput.setText("");
        pickupTimeInput.setText("");
        dropOffTimeInput.setText("");
        fetchRoutes(); // Refresh the route list
    }

    private void showTimePickerDialog(EditText targetEditText) {
        // Open at the time already in the box (not always 12:00).
        int[] start = RouteTimes.parse(targetEditText.getText().toString());
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> targetEditText.setText(RouteTimes.format(hourOfDay, minute)),
                start[0], start[1], true); // 24-hour format
        timePickerDialog.show();
    }
}

