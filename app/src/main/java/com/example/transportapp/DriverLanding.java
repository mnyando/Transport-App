package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DriverLanding extends AppCompatActivity {

    private TextView driverIdTextView, driverEmailTextView, driverNameTextView,
            vehicleTextView, routeTextView;
    private ImageView driverImageView;
    private Button pickUpButton, dropOffButton, messagesButton, sosButton;
    private FirebaseFirestore db;
    private String driverId, driverEmail, driverName, assignedVehicle, assignedRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_landing);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        driverId = intent.getStringExtra("driverId");
        driverEmail = intent.getStringExtra("driverEmail");
        driverName = intent.getStringExtra("driverName");
        assignedVehicle = intent.getStringExtra("assignedVehicle");
        assignedRoute = intent.getStringExtra("assignedRoute");

        if (driverId == null) {
            Toast.makeText(this, "Driver information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Stop sharing the bus position if a trip is still running when the driver signs out.
        Session.setUpHomeToolbar(this, () -> {
            if (assignedVehicle != null) TripLocationService.stop(this, assignedVehicle);
        });

        initializeViews();
        displayDriverInfo();
        setupButtonListeners();
    }

    private void initializeViews() {
        driverIdTextView = findViewById(R.id.driverIdTextView);
        driverEmailTextView = findViewById(R.id.driverEmailTextView);
        driverNameTextView = findViewById(R.id.driverNameTextView);
        vehicleTextView = findViewById(R.id.vehicleTextView);
        routeTextView = findViewById(R.id.routeTextView);
        driverImageView = findViewById(R.id.driverImageView);
        pickUpButton = findViewById(R.id.pickUpButton);
        dropOffButton = findViewById(R.id.dropOffButton);
        messagesButton = findViewById(R.id.messagesButton);
        sosButton = findViewById(R.id.sosButton);
    }

    private void displayDriverInfo() {
        driverIdTextView.setText("Driver ID: " + (driverId != null ? driverId : "N/A"));
        driverEmailTextView.setText("Email: " + (driverEmail != null ? driverEmail : "N/A"));
        driverNameTextView.setText("Driver Name: " + (driverName != null ? driverName : "N/A"));
        vehicleTextView.setText("Assigned Vehicle: " + (assignedVehicle != null ? assignedVehicle : "N/A"));
        routeTextView.setText("Assigned Route: " + (assignedRoute != null ? assignedRoute : "N/A"));
    }

    private boolean requireVehicle() {
        if (assignedVehicle == null || assignedVehicle.isEmpty()) {
            Toast.makeText(this, "No vehicle assigned to you", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupButtonListeners() {
        pickUpButton.setOnClickListener(v -> {
            if (requireVehicle()) startActivity(tripIntent(PickUp.class));
        });
        dropOffButton.setOnClickListener(v -> {
            if (requireVehicle()) startActivity(tripIntent(DropOff.class));
        });
        messagesButton.setOnClickListener(v -> {
            if (!requireVehicle()) return;
            Intent chat = new Intent(this, ChatActivity.class);
            chat.putExtra("vehicle", assignedVehicle);
            chat.putExtra("senderRole", "driver");
            chat.putExtra("senderName", driverName);
            startActivity(chat);
        });
        sosButton.setOnClickListener(v -> {
            if (requireVehicle()) confirmSos();
        });
    }

    private Intent tripIntent(Class<?> screen) {
        Intent trip = new Intent(this, screen);
        trip.putExtra("assignedVehicle", assignedVehicle);
        trip.putExtra("driverId", driverId);
        trip.putExtra("driverName", driverName);
        trip.putExtra("assignedRoute", assignedRoute);
        return trip;
    }

    /** SOS asks once (so a pocket tap doesn't alarm every parent) and takes an optional note. */
    private void confirmSos() {
        EditText details = new EditText(this);
        details.setHint("What happened? (optional)");
        details.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle("Send emergency alert?")
                .setMessage("The school and every parent with a child on bus " + assignedVehicle
                        + " will be alerted immediately.")
                .setView(details)
                .setPositiveButton("Send SOS", (d, w) -> sendSos(details.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendSos(String note) {
        sosButton.setEnabled(false);
        db.collectionGroup("studentList")
                .whereEqualTo("vehicle", assignedVehicle)
                .get()
                .addOnCompleteListener(task -> {
                    List<Student> students = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Student s = doc.toObject(Student.class);
                            s.setId(doc.getId());
                            students.add(s);
                        }
                    }
                    // The school alert is sent even if the student list could not be loaded.
                    int parents = TripAlerts.sendSos(db, students, assignedVehicle, driverId, driverName, note);
                    sosButton.setEnabled(true);
                    new AlertDialog.Builder(this)
                            .setTitle("SOS sent")
                            .setMessage("The school has been alerted"
                                    + (parents > 0 ? " and " + parents + " parent(s) notified." : ".")
                                    + " If anyone is in danger, also call emergency services.")
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDriverInfo();
    }

    /** Drivers live in the staff collection (the old code read a "drivers" collection nothing writes). */
    private void refreshDriverInfo() {
        if (driverId == null) return;
        db.collection("staff").document(driverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String vehicle = documentSnapshot.getString("assignedVehicle");
                        String route = documentSnapshot.getString("assignedRoute");
                        if (vehicle != null) assignedVehicle = vehicle;
                        if (route != null) assignedRoute = route;
                        displayDriverInfo();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to refresh driver info", Toast.LENGTH_SHORT).show());
    }
}
