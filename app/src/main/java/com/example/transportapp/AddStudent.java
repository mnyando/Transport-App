package com.example.transportapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStudent extends AppCompatActivity {

    private static final String TAG = "AddStudent";
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Spinner routeDropdown, studentClassInput;
    private Button saveStudentButton;
    private FirebaseFirestore db;
    private List<String> routeList;
    private ArrayAdapter<String> routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        studentNameInput = findViewById(R.id.studentNameInput);
        studentClassInput = findViewById(R.id.classDropdown);
        studentIdInput = findViewById(R.id.studentIdInput);
        parentNameInput = findViewById(R.id.parentNameInput);
        parentContactInput = findViewById(R.id.parentContactInput);
        homeLocationInput = findViewById(R.id.homeLocationInput);
        routeDropdown = findViewById(R.id.routeDropdown);
        saveStudentButton = findViewById(R.id.saveStudentButton);

        // Setup Grade Levels Spinner
        ArrayAdapter<CharSequence> gradeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.grade_levels,
                android.R.layout.simple_spinner_item
        );
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        studentClassInput.setAdapter(gradeAdapter);

        // Setup Routes Spinner
        routeList = new ArrayList<>();
        routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeDropdown.setAdapter(routeAdapter);
        fetchRoutes();

        // Save Button Click Handler
        saveStudentButton.setOnClickListener(v -> checkAndSaveStudentData());
    }

    private void fetchRoutes() {
        db.collection("Routes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String routeName = doc.getString("routeName");
                        if (routeName != null) {
                            routeList.add(routeName);
                        }
                    }
                    routeAdapter.notifyDataSetChanged();
                    if (routeList.isEmpty()) {
                        Toast.makeText(this, "No routes found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAndSaveStudentData() {
        String studentName = studentNameInput.getText().toString().trim();
        String studentClass = studentClassInput.getSelectedItem() != null ? studentClassInput.getSelectedItem().toString() : "";
        String studentId = studentIdInput.getText().toString().trim();
        String parentName = parentNameInput.getText().toString().trim();
        String parentContact = parentContactInput.getText().toString().trim();
        String homeLocation = homeLocationInput.getText().toString().trim();
        String route = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";

        if (studentName.isEmpty() || studentClass.isEmpty() || studentId.isEmpty() ||
                parentName.isEmpty() || parentContact.isEmpty() || homeLocation.isEmpty() || route.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select a class and route", Toast.LENGTH_SHORT).show();
            return;
        }

        checkVehicleCapacity(route);
    }

    private void checkVehicleCapacity(String routeName) {
        db.collection("Routes")
                .whereEqualTo("routeName", routeName)
                .limit(1)
                .get()
                .addOnSuccessListener(routeQuery -> {
                    if (!routeQuery.isEmpty()) {
                        DocumentSnapshot routeDoc = routeQuery.getDocuments().get(0);
                        String vehicleName = routeDoc.getString("vehicle");
                        String pickupTime = routeDoc.getString("pickupTime");
                        String dropOffTime = routeDoc.getString("dropOffTime");

                        if (vehicleName == null) {
                            Toast.makeText(this, "No vehicle assigned to this route", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get vehicle capacity
                        db.collection("vehicle")
                                .whereEqualTo("vehicleName", vehicleName)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(vehicleQuery -> {
                                    if (!vehicleQuery.isEmpty()) {
                                        String capacityStr = vehicleQuery.getDocuments().get(0).getString("capacity");
                                        int capacity;
                                        try {
                                            capacity = Integer.parseInt(capacityStr);
                                        } catch (NumberFormatException e) {
                                            Toast.makeText(this, "Invalid vehicle capacity", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // Count students on this route
                                        db.collectionGroup("studentList")
                                                .whereEqualTo("route", routeName)
                                                .get()
                                                .addOnSuccessListener(studentQuery -> {
                                                    int studentCount = studentQuery.size();
                                                    if (studentCount >= capacity) {
                                                        Toast.makeText(this, "Vehicle capacity (" + capacity + ") exceeded. Current students: " + studentCount, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        fetchStaffForVehicle(vehicleName, (driver, attendant) -> {
                                                            saveStudentData(
                                                                    studentNameInput.getText().toString().trim(),
                                                                    studentClassInput.getSelectedItem().toString(),
                                                                    studentIdInput.getText().toString().trim(),
                                                                    parentNameInput.getText().toString().trim(),
                                                                    parentContactInput.getText().toString().trim(),
                                                                    homeLocationInput.getText().toString().trim(),
                                                                    routeName,
                                                                    vehicleName,
                                                                    pickupTime,
                                                                    dropOffTime,
                                                                    driver,
                                                                    attendant
                                                            );
                                                        });
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void fetchStaffForVehicle(String vehicleName, StaffFetchCallback callback) {
        // Get driver
        db.collection("staff")
                .whereEqualTo("assignedVehicle", vehicleName)
                .whereEqualTo("role", "Driver")
                .whereEqualTo("status", "Active")
                .limit(1)
                .get()
                .addOnSuccessListener(driverQuery -> {
                    Map<String, String> driver = new HashMap<>();
                    if (!driverQuery.isEmpty()) {
                        DocumentSnapshot driverDoc = driverQuery.getDocuments().get(0);
                        driver.put("name", driverDoc.getString("staffName"));
                        driver.put("phone", driverDoc.getString("phoneNumber"));
                    }

                    // Get attendant
                    db.collection("staff")
                            .whereEqualTo("assignedVehicle", vehicleName)
                            .whereEqualTo("role", "Attendant")
                            .whereEqualTo("status", "Active")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(attendantQuery -> {
                                Map<String, String> attendant = new HashMap<>();
                                if (!attendantQuery.isEmpty()) {
                                    DocumentSnapshot attendantDoc = attendantQuery.getDocuments().get(0);
                                    attendant.put("name", attendantDoc.getString("staffName"));
                                    attendant.put("phone", attendantDoc.getString("phoneNumber"));
                                }

                                callback.onStaffFetched(driver, attendant);
                            });
                });
    }

    private void saveStudentData(
            String studentName, String studentClass, String studentId,
            String parentName, String parentContact, String homeLocation,
            String route, String vehicle, String pickupTime, String dropOffTime,
            Map<String, String> driver, Map<String, String> attendant
    ) {
        Map<String, Object> studentData = new HashMap<>();
        // Basic student info
        studentData.put("parentName", parentName);
        studentData.put("name", studentName);
        studentData.put("studentId", studentId);
        studentData.put("route", route);
        studentData.put("parentContact", parentContact);
        studentData.put("homeLocation", homeLocation);

        // Route and vehicle info
        studentData.put("vehicle", vehicle);
        studentData.put("pickupTime", pickupTime);
        studentData.put("dropOffTime", dropOffTime);

        // Staff info
        if (driver != null && !driver.isEmpty()) {
            studentData.put("driverName", driver.get("name"));
            studentData.put("driverPhone", driver.get("phone"));
        }
        if (attendant != null && !attendant.isEmpty()) {
            studentData.put("attendantName", attendant.get("name"));
            studentData.put("attendantPhone", attendant.get("phone"));
        }

        db.collection("students")
                .document(studentClass)
                .collection("studentList")
                .document(studentId)
                .set(studentData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Student added successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        studentNameInput.setText("");
        studentIdInput.setText("");
        parentNameInput.setText("");
        parentContactInput.setText("");
        homeLocationInput.setText("");
        studentClassInput.setSelection(0);
        routeDropdown.setSelection(0);
    }

    interface StaffFetchCallback {
        void onStaffFetched(Map<String, String> driver, Map<String, String> attendant);
    }
}