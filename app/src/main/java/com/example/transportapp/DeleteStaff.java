package com.example.transportapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteStaff extends AppCompatActivity implements StaffAdapter.OnStaffClickListener {

    private EditText staffNameInput, staffPhoneInput, licenseNumberInput;
    private Spinner vehicleDropdown, routeDropdown;
    private RadioGroup statusRadioGroup, roleRadioGroup; // Added roleRadioGroup
    private Button deleteStaffButton;
    private RecyclerView staffRecyclerView;
    private StaffAdapter staffAdapter;
    private List<Staff> staffList;
    private FirebaseFirestore firestore;
    private String selectedStaffId = null;
    private List<String> vehicleList;
    private List<String> routeList;
    private ArrayAdapter<String> vehicleAdapter;
    private ArrayAdapter<String> routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        staffNameInput = findViewById(R.id.driverNameInput);
        staffPhoneInput = findViewById(R.id.driverPhoneInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        roleRadioGroup = findViewById(R.id.roleRadioGroup); // Initialize new RadioGroup
        deleteStaffButton = findViewById(R.id.deleteStaffButton); // Assuming ID updated in XML
        staffRecyclerView = findViewById(R.id.driverRecyclerView);

        // Setup RecyclerView
        staffList = new ArrayList<>();
        staffAdapter = new StaffAdapter(staffList, this);
        staffRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        staffRecyclerView.setAdapter(staffAdapter);

        // Setup Spinners
        vehicleList = new ArrayList<>();
        routeList = new ArrayList<>();
        vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleList);
        routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleDropdown.setAdapter(vehicleAdapter);
        routeDropdown.setAdapter(routeAdapter);

        // Load data
        fetchVehicles();
        fetchRoutes();
        fetchStaff();

        // Handle staff deletion
        deleteStaffButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void fetchStaff() {
        firestore.collection("staff") // Changed to "staff"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    staffList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Staff staff = doc.toObject(Staff.class);
                        staff.setId(doc.getId());
                        staffList.add(staff);
                    }
                    staffAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchVehicles() {
        firestore.collection("vehicle")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicleList.clear();
                    vehicleList.add("Select Vehicle");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String vehicleName = doc.getString("vehicleName");
                        if (vehicleName != null) {
                            vehicleList.add(vehicleName);
                        }
                    }
                    vehicleAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchRoutes() {
        firestore.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    routeList.add("Select Route");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String routeName = doc.getString("routeName");
                        if (routeName != null) {
                            routeList.add(routeName);
                        }
                    }
                    routeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStaffClick(Staff staff) {
        selectedStaffId = staff.getId();
        staffNameInput.setText(staff.getStaffName());
        staffPhoneInput.setText(staff.getPhoneNumber());
        licenseNumberInput.setText(staff.getLicenseNumber());

        // Set Spinner selections
        int vehiclePosition = vehicleAdapter.getPosition(staff.getAssignedVehicle());
        vehicleDropdown.setSelection(vehiclePosition >= 0 ? vehiclePosition : 0);

        int routePosition = routeAdapter.getPosition(staff.getAssignedRoute());
        routeDropdown.setSelection(routePosition >= 0 ? routePosition : 0);

        // Set Status RadioGroup selection
        if ("Active".equals(staff.getStatus())) {
            statusRadioGroup.check(R.id.activeRadioButton);
        } else if ("Not Active".equals(staff.getStatus())) {
            statusRadioGroup.check(R.id.notActiveRadioButton);
        } else {
            statusRadioGroup.clearCheck();
        }

        // Set Role RadioGroup selection
        if ("Driver".equals(staff.getRole())) {
            roleRadioGroup.check(R.id.driverRadioButton);
        } else if ("Attendant".equals(staff.getRole())) {
            roleRadioGroup.check(R.id.attendantRadioButton);
        } else {
            roleRadioGroup.check(R.id.driverRadioButton); // Default to Driver if null
        }
    }

    private void showDeleteConfirmationDialog() {
        if (selectedStaffId == null) {
            Toast.makeText(this, "Please select a staff member to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Staff")
                .setMessage("Are you sure you want to delete " + staffNameInput.getText().toString() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteStaff())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStaff() {
        firestore.collection("staff").document(selectedStaffId) // Changed to "staff"
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Staff deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchStaff(); // Refresh the list
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearFields() {
        selectedStaffId = null;
        staffNameInput.setText("");
        staffPhoneInput.setText("");
        licenseNumberInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
        statusRadioGroup.check(R.id.activeRadioButton); // Reset to default "Active"
        roleRadioGroup.check(R.id.driverRadioButton); // Reset to default "Driver"
    }
}