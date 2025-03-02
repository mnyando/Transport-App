package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStudent extends AppCompatActivity {

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

        // Setup Grade Levels Spinner (from strings.xml)
        ArrayAdapter<CharSequence> gradeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.grade_levels,
                android.R.layout.simple_spinner_item
        );
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        studentClassInput.setAdapter(gradeAdapter);

        // Setup Routes Spinner (from Firestore)
        routeList = new ArrayList<>();
        routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeDropdown.setAdapter(routeAdapter);
        fetchRoutes();

        // Handle Save Button
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

        // Check vehicle capacity before saving
        checkVehicleCapacity(route, () -> saveStudentData(studentName, studentClass, studentId, parentName, parentContact, homeLocation, route));
    }

    private void checkVehicleCapacity(String routeName, Runnable onSuccess) {
        // Step 1: Get the vehicle assigned to the route
        db.collection("Routes")
                .whereEqualTo("routeName", routeName)
                .limit(1)
                .get()
                .addOnSuccessListener(routeQuery -> {
                    if (!routeQuery.isEmpty()) {
                        String vehicleName = routeQuery.getDocuments().get(0).getString("vehicle");
                        if (vehicleName == null) {
                            Toast.makeText(this, "No vehicle assigned to this route", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Step 2: Get the vehicle's capacity
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

                                        // Step 3: Count students assigned to this vehicle
                                        db.collectionGroup("studentList")
                                                .whereEqualTo("route", routeName)
                                                .get()
                                                .addOnSuccessListener(studentQuery -> {
                                                    int studentCount = studentQuery.size();
                                                    if (studentCount >= capacity) {
                                                        Toast.makeText(this, "Vehicle capacity (" + capacity + ") exceeded. Current students: " + studentCount, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        onSuccess.run(); // Proceed with saving
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "Error counting students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error fetching vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveStudentData(String studentName, String studentClass, String studentId, String parentName,
                                 String parentContact, String homeLocation, String route) {
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("name", studentName);
        studentData.put("studentId", studentId);
        studentData.put("route", route);
        studentData.put("parentName", parentName);
        studentData.put("parentContact", parentContact);
        studentData.put("homeLocation", homeLocation);

        db.collection("students").document(studentClass)
                .collection("studentList").document(studentId)
                .set(studentData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}