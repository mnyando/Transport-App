package com.example.transportapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyStaff extends AppCompatActivity implements StaffAdapter.OnStaffClickListener {

    private static final String TAG = "ModifyStaff"; // For logging

    private EditText staffNameInput, staffPhoneInput, staffEmailInput, licenseNumberInput;
    private Spinner vehicleDropdown, routeDropdown;
    private RadioGroup statusRadioGroup, roleRadioGroup;
    private Button updateStaffButton;
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
        setContentView(R.layout.activity_modify_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        staffNameInput = findViewById(R.id.driverNameInput);
        staffPhoneInput = findViewById(R.id.driverPhoneInput);
        staffEmailInput = findViewById(R.id.staffEmailInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        vehicleDropdown = findViewById(R.id.vehicleDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        updateStaffButton = findViewById(R.id.saveDriverButton);
        staffRecyclerView = findViewById(R.id.driverRecyclerView);

        // Setup RecyclerView
        staffList = new ArrayList<>();
        staffAdapter = new StaffAdapter(staffList, this);
        staffRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        staffRecyclerView.setAdapter(staffAdapter);
        Log.d(TAG, "RecyclerView initialized with adapter");

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

        // Handle staff updates
        updateStaffButton.setOnClickListener(v -> updateStaff());
    }

    private void fetchStaff() {
        Log.d(TAG, "Fetching staff from Firestore");
        firestore.collection(FirestorePaths.STAFF)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    staffList.clear();
                    Log.d(TAG, "Firestore query returned " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Staff staff = doc.toObject(Staff.class);
                        staff.setId(doc.getId());
                        staffList.add(staff);
                        Log.d(TAG, "Added staff: " + staff.getStaffName() + ", Role: " + staff.getRole());
                    }
                    staffAdapter.notifyDataSetChanged();
                    if (staffList.isEmpty()) {
                        Toast.makeText(this, "No staff found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching staff: " + e.getMessage());
                    UserErrors.show(this, "Could not load staff", e);
                });
    }

    private void fetchVehicles() {
        firestore.collection(FirestorePaths.VEHICLES)
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
                    Log.e(TAG, "Error fetching vehicles: " + e.getMessage());
                    UserErrors.show(this, "Could not load vehicles", e);
                });
    }

    private void fetchRoutes() {
        firestore.collection(FirestorePaths.ROUTES)
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
                    Log.e(TAG, "Error fetching routes: " + e.getMessage());
                    UserErrors.show(this, "Could not load routes", e);
                });
    }

    @Override
    public void onStaffClick(Staff staff) {
        selectedStaffId = staff.getId();
        staffNameInput.setText(staff.getStaffName());
        staffPhoneInput.setText(staff.getPhoneNumber());
        staffEmailInput.setText(staff.getEmail());
        licenseNumberInput.setText(staff.getLicenseNumber());

        int vehiclePosition = vehicleAdapter.getPosition(staff.getAssignedVehicle());
        vehicleDropdown.setSelection(vehiclePosition >= 0 ? vehiclePosition : 0);

        int routePosition = routeAdapter.getPosition(staff.getAssignedRoute());
        routeDropdown.setSelection(routePosition >= 0 ? routePosition : 0);

        if ("Active".equals(staff.getStatus())) {
            statusRadioGroup.check(R.id.activeRadioButton);
        } else if ("Not Active".equals(staff.getStatus())) {
            statusRadioGroup.check(R.id.notActiveRadioButton);
        } else {
            statusRadioGroup.clearCheck();
        }

        if ("Driver".equals(staff.getRole())) {
            roleRadioGroup.check(R.id.driverRadioButton);
        } else if ("Attendant".equals(staff.getRole())) {
            roleRadioGroup.check(R.id.attendantRadioButton);
        } else {
            roleRadioGroup.check(R.id.driverRadioButton);
        }
    }

    private void updateStaff() {
        if (selectedStaffId == null) {
            Toast.makeText(this, "Please select a staff member to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedName = staffNameInput.getText().toString().trim();
        String updatedPhone = staffPhoneInput.getText().toString().trim();
        String updatedEmail = ParentEmails.normalize(staffEmailInput.getText().toString());
        String updatedLicense = licenseNumberInput.getText().toString().trim();
        String updatedVehicle = vehicleDropdown.getSelectedItem() != null ? vehicleDropdown.getSelectedItem().toString() : "";
        String updatedRoute = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";
        int selectedStatusId = statusRadioGroup.getCheckedRadioButtonId();
        if (selectedStatusId == -1) {
            // Records with no stored status clear the toggle; don't silently save "Not Active".
            Toast.makeText(this, R.string.error_select_status, Toast.LENGTH_SHORT).show();
            return;
        }
        String updatedStatus = selectedStatusId == R.id.activeRadioButton ? "Active" : "Not Active";
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        String updatedRole = selectedRoleId == R.id.driverRadioButton ? "Driver" : "Attendant";

        if (TextUtils.isEmpty(updatedName) || TextUtils.isEmpty(updatedPhone) ||
                "Select Vehicle".equals(updatedVehicle) || "Select Route".equals(updatedRoute)) {
            Toast.makeText(this, "All fields are required and must be valid", Toast.LENGTH_SHORT).show();
            return;
        }
        // Only drivers need a driving licence (conductors were blocked by this before).
        if ("Driver".equals(updatedRole) && TextUtils.isEmpty(updatedLicense)) {
            licenseNumberInput.setError(getString(R.string.error_license_required_driver));
            licenseNumberInput.requestFocus();
            return;
        }

        if ("Driver".equals(updatedRole) && !ParentEmails.isValid(updatedEmail)) {
            staffEmailInput.setError(getString(R.string.staff_email_required));
            staffEmailInput.requestFocus();
            return;
        }
        if (!updatedEmail.isEmpty() && !ParentEmails.isValid(updatedEmail)) {
            staffEmailInput.setError(getString(R.string.staff_email_invalid));
            staffEmailInput.requestFocus();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("staffName", updatedName);
        updatedData.put("phoneNumber", updatedPhone);
        updatedData.put("email", updatedEmail);
        updatedData.put("licenseNumber", updatedLicense);
        updatedData.put("assignedVehicle", updatedVehicle);
        updatedData.put("assignedRoute", updatedRoute);
        updatedData.put("status", updatedStatus);
        updatedData.put("role", updatedRole);

        // Same rules as Add Staff, ignoring the record being edited: licence numbers are unique
        // and a bus has at most 2 drivers. (Editing used to skip both.)
        String staffId = selectedStaffId;
        Task<QuerySnapshot> licenceTask = updatedLicense.isEmpty() ? Tasks.forResult(null)
                : firestore.collection(FirestorePaths.STAFF)
                        .whereEqualTo("licenseNumber", updatedLicense).limit(2).get();
        Task<QuerySnapshot> driversTask = "Driver".equals(updatedRole)
                ? firestore.collection(FirestorePaths.STAFF)
                        .whereEqualTo("assignedVehicle", updatedVehicle)
                        .whereEqualTo("role", "Driver").get()
                : Tasks.forResult(null);

        updateStaffButton.setEnabled(false);
        Tasks.whenAllComplete(licenceTask, driversTask).addOnCompleteListener(all -> {
            if (!licenceTask.isSuccessful() || !driversTask.isSuccessful()) {
                updateStaffButton.setEnabled(true);
                UserErrors.show(this, "Could not update staff",
                        !licenceTask.isSuccessful() ? licenceTask.getException() : driversTask.getException());
                return;
            }
            if (licenceTask.getResult() != null) for (DocumentSnapshot doc : licenceTask.getResult()) {
                if (!doc.getId().equals(staffId)) {
                    updateStaffButton.setEnabled(true);
                    licenseNumberInput.setError(getString(R.string.error_license_taken));
                    licenseNumberInput.requestFocus();
                    return;
                }
            }
            if (driversTask.getResult() != null) {
                int otherDrivers = 0;
                for (DocumentSnapshot doc : driversTask.getResult()) {
                    if (!doc.getId().equals(staffId)) otherDrivers++;
                }
                if (otherDrivers >= 2) {
                    updateStaffButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_driver_limit, updatedVehicle),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            firestore.collection(FirestorePaths.STAFF).document(staffId)
                    .update(updatedData)
                    .addOnSuccessListener(aVoid -> {
                        updateStaffButton.setEnabled(true);
                        Toast.makeText(this, "Staff updated successfully", Toast.LENGTH_SHORT).show();
                        clearForm();
                        fetchStaff();
                    })
                    .addOnFailureListener(e -> {
                        updateStaffButton.setEnabled(true);
                        Log.e(TAG, "Failed to update staff: " + e.getMessage());
                        UserErrors.show(this, "Could not update staff", e);
                    });
        });
    }

    private void clearForm() {
        staffNameInput.setText("");
        staffPhoneInput.setText("");
        staffEmailInput.setText("");
        licenseNumberInput.setText("");
        vehicleDropdown.setSelection(0);
        routeDropdown.setSelection(0);
        statusRadioGroup.check(R.id.activeRadioButton);
        roleRadioGroup.check(R.id.driverRadioButton);
        selectedStaffId = null;
    }
}

