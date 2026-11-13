package com.example.transportapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStaff extends AppCompatActivity {

    private EditText driverNameInput, driverPhoneInput, staffEmailInput, licenseNumberInput;
    private Spinner vehicleDropdown, routeDropdown;
    private RadioGroup statusRadioGroup, roleRadioGroup; // Added roleRadioGroup
    private RadioButton driverRadioButton, attendantRadioButton; // For role selection
    private Button saveDriverButton;
    private FirebaseFirestore db;
    private List<String> vehicleList = new ArrayList<>();
    private List<String> routeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        driverNameInput = findViewById(R.id.driverNameInput);
        driverPhoneInput = findViewById(R.id.driverPhoneInput);
        staffEmailInput = findViewById(R.id.staffEmailInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        roleRadioGroup = findViewById(R.id.roleRadioGroup); // Initialize new RadioGroup
        driverRadioButton = findViewById(R.id.driverRadioButton); // Initialize Driver button
        attendantRadioButton = findViewById(R.id.attendantRadioButton); // Initialize Attendant button
        saveDriverButton = findViewById(R.id.saveDriverButton);

        // Load data for spinners
        loadVehicles();
        loadRoutes();

        // Handle Save button click
        saveDriverButton.setOnClickListener(v -> saveStaffData()); // Renamed method for clarity
    }

    private void loadVehicles() {
        CollectionReference vehicleRef = db.collection(FirestorePaths.VEHICLES);
        vehicleRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            vehicleList.clear();
            vehicleList.add("Select Vehicle");
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String vehicleName = doc.getString("vehicleName");
                if (vehicleName != null) {
                    vehicleList.add(vehicleName);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vehicleDropdown.setAdapter(adapter);
        }).addOnFailureListener(e -> {
            UserErrors.show(this, "Could not load vehicles", e);
        });
    }

    private void loadRoutes() {
        CollectionReference routeRef = db.collection(FirestorePaths.ROUTES);
        routeRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            routeList.clear();
            routeList.add("Select Route");
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String routeName = doc.getString("routeName");
                if (routeName != null) {
                    routeList.add(routeName);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeDropdown.setAdapter(adapter);
        }).addOnFailureListener(e -> {
            UserErrors.show(this, "Could not load routes", e);
        });
    }

    private void saveStaffData() {
        String staffName = driverNameInput.getText().toString().trim();
        String staffPhone = driverPhoneInput.getText().toString().trim();
        String staffEmail = ParentEmails.normalize(staffEmailInput.getText().toString());
        String licenseNumber = licenseNumberInput.getText().toString().trim();
        String assignedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String assignedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";
        int selectedStatusId = statusRadioGroup.getCheckedRadioButtonId();
        String status = selectedStatusId == R.id.activeRadioButton ? "Active" : "Not Active";
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        String role = selectedRoleId == R.id.driverRadioButton ? "Driver" : "Attendant";

        if (staffName.isEmpty() || staffPhone.isEmpty() || licenseNumber.isEmpty() ||
                "Select Vehicle".equals(assignedVehicle) || "Select Route".equals(assignedRoute)) {
            Toast.makeText(this, "Please fill out all fields and select a vehicle and route", Toast.LENGTH_SHORT).show();
            return;
        }

        // A driver's login email links their app account to this record (and only this bus).
        if ("Driver".equals(role) && !ParentEmails.isValid(staffEmail)) {
            staffEmailInput.setError(getString(R.string.staff_email_required));
            staffEmailInput.requestFocus();
            return;
        }
        if (!staffEmail.isEmpty() && !ParentEmails.isValid(staffEmail)) {
            staffEmailInput.setError(getString(R.string.staff_email_invalid));
            staffEmailInput.requestFocus();
            return;
        }

        // Check staff count for the selected vehicle
        checkStaffCountForVehicle(assignedVehicle, () -> {
            Map<String, Object> staffData = new HashMap<>();
            staffData.put("staffName", staffName);
            staffData.put("phoneNumber", staffPhone);
            staffData.put("email", staffEmail);
            staffData.put("licenseNumber", licenseNumber);
            staffData.put("assignedVehicle", assignedVehicle);
            staffData.put("assignedRoute", assignedRoute);
            staffData.put("status", status);
            staffData.put("role", role);

            // Generated document IDs (review #13): a licence number can change or be mistyped,
            // and it used to be the record's key. Older staff keep their licence-number IDs.
            db.collection(FirestorePaths.STAFF).whereEqualTo("licenseNumber", licenseNumber).limit(1).get()
                    .addOnSuccessListener(existing -> {
                        if (!existing.isEmpty()) {
                            licenseNumberInput.setError(getString(R.string.error_license_taken));
                            licenseNumberInput.requestFocus();
                            return;
                        }
                        db.collection(FirestorePaths.STAFF).add(staffData)
                                .addOnSuccessListener(ref -> {
                                    Toast.makeText(this, R.string.staff_added, Toast.LENGTH_SHORT).show();
                                    clearForm();
                                })
                                .addOnFailureListener(e -> UserErrors.show(this, "Could not save staff", e));
                    })
                    .addOnFailureListener(e -> UserErrors.show(this, "Could not save staff", e));
        });
    }

    private void checkStaffCountForVehicle(String vehicleName, Runnable onSuccess) {
        db.collection(FirestorePaths.STAFF)
                .whereEqualTo("assignedVehicle", vehicleName)
                .whereEqualTo("role", "Driver") // Only count drivers
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int driverCount = queryDocumentSnapshots.size();
                    if (driverCount >= 2 && driverRadioButton.isChecked()) {
                        Toast.makeText(this, "Maximum of 2 drivers already assigned to " + vehicleName, Toast.LENGTH_SHORT).show();
                    } else {
                        onSuccess.run(); // Proceed with saving
                    }
                })
                .addOnFailureListener(e -> {
                    UserErrors.show(this, "Could not check staff count", e);
                });
    }

    private void clearForm() {
        driverNameInput.setText("");
        driverPhoneInput.setText("");
        staffEmailInput.setText("");
        licenseNumberInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
        statusRadioGroup.check(R.id.activeRadioButton); // Reset to "Active"
        roleRadioGroup.check(R.id.driverRadioButton); // Reset to "Driver"
    }
}