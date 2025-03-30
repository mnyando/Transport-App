package com.example.transportapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyStudent extends AppCompatActivity {
    private static final String TAG = "ModifyStudent";
    private FirebaseFirestore db;
    private Spinner filterDropdown, classDropdown, routeDropdown;
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Button saveStudentButton;
    private RecyclerView studentRecyclerView;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private String selectedStudentId = null;
    private String originalGrade = null;
    private List<String> routeList;
    private ArrayAdapter<String> routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_student);
        Log.i(TAG, "onCreate: Activity started");

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Firestore initialized");

        // Initialize Views
        filterDropdown = findViewById(R.id.filterDropdown);
        classDropdown = findViewById(R.id.classDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        studentNameInput = findViewById(R.id.studentNameInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        parentNameInput = findViewById(R.id.parentNameInput);
        parentContactInput = findViewById(R.id.parentContactInput);
        homeLocationInput = findViewById(R.id.homeLocationInput);
        saveStudentButton = findViewById(R.id.saveStudentButton);
        studentRecyclerView = findViewById(R.id.studentRecyclerView);
        Log.d(TAG, "onCreate: Views initialized");

        // Setup RecyclerView
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(studentList, this::onStudentClicked);
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentRecyclerView.setAdapter(studentAdapter);
        Log.d(TAG, "onCreate: RecyclerView setup complete");

        // Load filter dropdown (grades for filtering students) from strings.xml
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.grade_levels, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterDropdown.setAdapter(filterAdapter);
        Log.i(TAG, "onCreate: filterDropdown initialized with " + filterAdapter.getCount() + " items");

        // Load class dropdown (grades for editing student class) from strings.xml
        ArrayAdapter<CharSequence> classAdapter = ArrayAdapter.createFromResource(this,
                R.array.grade_levels, android.R.layout.simple_spinner_item);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classDropdown.setAdapter(classAdapter);
        if (classAdapter.isEmpty()) {
            Log.w(TAG, "onCreate: classDropdown is empty, check strings.xml R.array.grade_levels");
            Toast.makeText(this, "No grade levels defined in strings.xml", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "onCreate: classDropdown initialized with " + classAdapter.getCount() + " items");
        }

        // Load route dropdown from Firestore
        routeList = new ArrayList<>();
        routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeDropdown.setAdapter(routeAdapter);
        Log.d(TAG, "onCreate: routeDropdown adapter initialized, fetching routes...");
        fetchRoutes();

        // Fetch students when a grade is selected in filterDropdown
        filterDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = parent.getItemAtPosition(position).toString();
                Log.i(TAG, "filterDropdown: Selected grade: " + selectedGrade);
                fetchStudentsByGrade(selectedGrade);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                studentList.clear();
                studentAdapter.notifyDataSetChanged();
                Log.d(TAG, "filterDropdown: Nothing selected, cleared student list");
            }
        });

        // Save Button Click
        saveStudentButton.setOnClickListener(v -> {
            Log.i(TAG, "saveStudentButton: Clicked");
            checkAndSaveStudentData();
        });
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
                    Log.i(TAG, "fetchRoutes: Successfully fetched " + routeList.size() + " routes");
                    if (routeList.isEmpty()) {
                        Log.w(TAG, "fetchRoutes: No routes found in Firestore");
                        Toast.makeText(this, "No routes found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchRoutes: Error fetching routes: " + e.getMessage(), e);
                    Toast.makeText(this, "Error fetching routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentsByGrade(String grade) {
        if (grade == null || grade.isEmpty()) {
            Log.w(TAG, "fetchStudentsByGrade: Grade is null or empty, aborting fetch");
            Toast.makeText(this, "Invalid grade selected", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "fetchStudentsByGrade: Fetching students for grade: " + grade);
        db.collection("students").document(grade)
                .collection("studentList")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        studentList.add(student);
                    }
                    studentAdapter.notifyDataSetChanged();
                    Log.i(TAG, "fetchStudentsByGrade: Fetched " + studentList.size() + " students for grade " + grade);
                    if (studentList.isEmpty()) {
                        Log.w(TAG, "fetchStudentsByGrade: No students found for " + grade);
                        Toast.makeText(this, "No students found for " + grade, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchStudentsByGrade: Error fetching students for grade " + grade + ": " + e.getMessage(), e);
                    Toast.makeText(this, "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onStudentClicked(Student student) {
        selectedStudentId = student.getId();
        originalGrade = student.getGrade() != null ? student.getGrade() :
                filterDropdown.getSelectedItem() != null ? filterDropdown.getSelectedItem().toString() : "";

        // Basic info
        studentNameInput.setText(student.getName());
        studentIdInput.setText(student.getStudentId());
        parentNameInput.setText(student.getParentName());
        parentContactInput.setText(student.getParentContact());
        homeLocationInput.setText(student.getHomeLocation());

        // Class selection
        ArrayAdapter<String> classAdapter = (ArrayAdapter<String>) classDropdown.getAdapter();
        int classPosition = classAdapter.getPosition(student.getGrade());
        if (classPosition >= 0) {
            classDropdown.setSelection(classPosition);
            Log.d(TAG, "onStudentClicked: Set classDropdown to " + student.getGrade());
        } else {
            classDropdown.setSelection(0);
            Log.w(TAG, "onStudentClicked: Grade " + student.getGrade() + " not found in classDropdown");
            Toast.makeText(this, "Grade " + student.getGrade() + " not found in list", Toast.LENGTH_SHORT).show();
        }

        // Route selection
        int routePosition = routeAdapter.getPosition(student.getRoute());
        if (routePosition >= 0) {
            routeDropdown.setSelection(routePosition);
            Log.d(TAG, "onStudentClicked: Set routeDropdown to " + student.getRoute());
        } else {
            routeDropdown.setSelection(0);
            Log.w(TAG, "onStudentClicked: Route " + student.getRoute() + " not found in routeDropdown");
            Toast.makeText(this, "Route " + student.getRoute() + " not found in list", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "onStudentClicked: Editing student " + student.getName() + " (ID: " + selectedStudentId + ", Original Grade: " + originalGrade + ")");
        Toast.makeText(this, "Editing " + student.getName() + "'s record", Toast.LENGTH_SHORT).show();
    }

    private void checkAndSaveStudentData() {
        Log.d(TAG, "checkAndSaveStudentData: Starting validation");
        String name = studentNameInput.getText().toString().trim();
        String studentId = studentIdInput.getText().toString().trim();
        String parentName = parentNameInput.getText().toString().trim();
        String parentContact = parentContactInput.getText().toString().trim();
        String homeLocation = homeLocationInput.getText().toString().trim();
        String grade = classDropdown.getSelectedItem() != null ? classDropdown.getSelectedItem().toString() : "";
        String route = routeDropdown.getSelectedItem() != null ? routeDropdown.getSelectedItem().toString() : "";

        if (name.isEmpty() || studentId.isEmpty() || parentName.isEmpty() || parentContact.isEmpty() ||
                homeLocation.isEmpty() || grade.isEmpty() || route.isEmpty()) {
            Log.w(TAG, "checkAndSaveStudentData: Validation failed - empty fields");
            Toast.makeText(this, "Please fill all fields and select a class and route", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("name", name);
        studentData.put("studentId", studentId);
        studentData.put("grade", grade);
        studentData.put("route", route);
        studentData.put("parentName", parentName);
        studentData.put("parentContact", parentContact);
        studentData.put("homeLocation", homeLocation);
        Log.d(TAG, "checkAndSaveStudentData: Student data prepared: " + studentData.toString());

        // Check vehicle capacity before saving
        Log.i(TAG, "checkAndSaveStudentData: Checking vehicle capacity for route: " + route);
        checkVehicleCapacity(route, () -> {
            if (selectedStudentId != null) {
                String originalStudentId = selectedStudentId;
                String currentFilterGrade = filterDropdown.getSelectedItem() != null ? filterDropdown.getSelectedItem().toString() : "";
                if (originalGrade != null && !originalGrade.isEmpty() && !originalGrade.equals(grade)) {
                    // Grade changed, delete from original grade and then save to new grade
                    Log.i(TAG, "checkAndSaveStudentData: Grade changed from " + originalGrade + " to " + grade);
                    deleteStudentFromOriginalGrade(originalGrade, originalStudentId, studentData, grade);
                } else if (currentFilterGrade != null && !currentFilterGrade.isEmpty() && !currentFilterGrade.equals(grade)) {
                    // Use filterDropdown grade if originalGrade is empty/null
                    Log.i(TAG, "checkAndSaveStudentData: Using filter grade " + currentFilterGrade + " as original, changed to " + grade);
                    deleteStudentFromOriginalGrade(currentFilterGrade, originalStudentId, studentData, grade);
                } else {
                    // No grade change or originalGrade is empty, update in place
                    Log.i(TAG, "checkAndSaveStudentData: No grade change or original grade empty, updating in place with ID " + originalStudentId);
                    saveStudentData(studentData, grade, originalStudentId);
                }
            } else {
                // New student, check if studentId already exists in the grade
                Log.i(TAG, "checkAndSaveStudentData: Adding new student with ID " + studentId);
                checkStudentIdExists(grade, studentId, () -> saveStudentData(studentData, grade, studentId));
            }
        });
    }

    private void checkStudentIdExists(String grade, String studentId, Runnable onSuccess) {
        Log.d(TAG, "checkStudentIdExists: Checking if studentId " + studentId + " exists in grade " + grade);
        db.collection("students").document(grade)
                .collection("studentList").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.w(TAG, "checkStudentIdExists: Student with ID " + studentId + " already exists in grade " + grade);
                        Toast.makeText(this, "A student with ID " + studentId + " already exists in " + grade, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "checkStudentIdExists: No conflict found, proceeding with save");
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkStudentIdExists: Error checking student ID: " + e.getMessage(), e);
                    Toast.makeText(this, "Error checking student ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteStudentFromOriginalGrade(String originalGrade, String studentId, Map<String, Object> studentData, String newGrade) {
        Log.d(TAG, "deleteStudentFromOriginalGrade: Deleting student " + studentId + " from grade " + originalGrade);
        if (originalGrade == null || originalGrade.isEmpty()) {
            Log.w(TAG, "deleteStudentFromOriginalGrade: Original grade is null or empty, proceeding with save to new grade " + newGrade);
            Toast.makeText(this, "Original grade not found, saving to new grade directly", Toast.LENGTH_SHORT).show();
            saveStudentData(studentData, newGrade, studentId);
            return;
        }

        if (studentId == null || studentId.isEmpty()) {
            Log.e(TAG, "deleteStudentFromOriginalGrade: Invalid student ID: " + studentId);
            Toast.makeText(this, "Invalid student ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("students").document(originalGrade)
                .collection("studentList").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "deleteStudentFromOriginalGrade: Student deleted from " + originalGrade + " successfully");
                    // After successful deletion, save to the new grade
                    saveStudentData(studentData, newGrade, studentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteStudentFromOriginalGrade: Failed to delete student from " + originalGrade + ": " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to delete student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkVehicleCapacity(String routeName, Runnable onSuccess) {
        Log.d(TAG, "checkVehicleCapacity: Querying route: " + routeName);
        db.collection("Routes")
                .whereEqualTo("routeName", routeName)
                .limit(1)
                .get()
                .addOnSuccessListener(routeQuery -> {
                    if (!routeQuery.isEmpty()) {
                        String vehicleName = routeQuery.getDocuments().get(0).getString("vehicle");
                        if (vehicleName == null) {
                            Log.w(TAG, "checkVehicleCapacity: No vehicle assigned to route " + routeName);
                            Toast.makeText(this, "No vehicle assigned to this route", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Log.d(TAG, "checkVehicleCapacity: Vehicle found: " + vehicleName);

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
                                            Log.d(TAG, "checkVehicleCapacity: Vehicle capacity: " + capacity);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "checkVehicleCapacity: Invalid vehicle capacity: " + capacityStr, e);
                                            Toast.makeText(this, "Invalid vehicle capacity", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        db.collectionGroup("studentList")
                                                .whereEqualTo("route", routeName)
                                                .get()
                                                .addOnSuccessListener(studentQuery -> {
                                                    int studentCount = studentQuery.size();
                                                    Log.i(TAG, "checkVehicleCapacity: Students assigned to route " + routeName + ": " + studentCount);
                                                    if (studentCount >= capacity) {
                                                        Log.w(TAG, "checkVehicleCapacity: Capacity exceeded - " + studentCount + "/" + capacity);
                                                        Toast.makeText(this, "Vehicle capacity (" + capacity + ") exceeded. Current students: " + studentCount, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Log.i(TAG, "checkVehicleCapacity: Capacity OK, proceeding with save");
                                                        onSuccess.run();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "checkVehicleCapacity: Error counting students: " + e.getMessage(), e);
                                                    Toast.makeText(this, "Error counting students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Log.w(TAG, "checkVehicleCapacity: Vehicle " + vehicleName + " not found");
                                        Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "checkVehicleCapacity: Error fetching vehicle " + vehicleName + ": " + e.getMessage(), e);
                                    Toast.makeText(this, "Error fetching vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "checkVehicleCapacity: Route " + routeName + " not found");
                        Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkVehicleCapacity: Error fetching route " + routeName + ": " + e.getMessage(), e);
                    Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveStudentData(Map<String, Object> studentData, String grade, String documentId) {
        Log.d(TAG, "saveStudentData: Saving data for grade: " + grade + ", documentId: " + documentId);

        // Get the selected route
        String routeName = routeDropdown.getSelectedItem() != null ?
                routeDropdown.getSelectedItem().toString() : "";

        if (routeName.isEmpty()) {
            Toast.makeText(this, "Please select a route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch route details first
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

                        // Add route and vehicle info to student data
                        studentData.put("vehicle", vehicleName);
                        studentData.put("pickupTime", pickupTime);
                        studentData.put("dropOffTime", dropOffTime);

                        // Fetch staff information
                        fetchStaffForVehicle(vehicleName, (driver, attendant) -> {
                            if (driver != null && !driver.isEmpty()) {
                                studentData.put("driverName", driver.get("name"));
                                studentData.put("driverPhone", driver.get("phone"));
                            }
                            if (attendant != null && !attendant.isEmpty()) {
                                studentData.put("attendantName", attendant.get("name"));
                                studentData.put("attendantPhone", attendant.get("phone"));
                            }

                            // Now save the complete student data
                            performFinalSave(studentData, grade, documentId);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching route details: " + e.getMessage());
                    Toast.makeText(this, "Error fetching route details", Toast.LENGTH_SHORT).show();
                });
    }

    private void performFinalSave(Map<String, Object> studentData, String grade, String documentId) {
        db.collection("students").document(grade)
                .collection("studentList").document(documentId)
                .set(studentData)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Student saved successfully");
                    Toast.makeText(this, "Student record updated successfully", Toast.LENGTH_SHORT).show();
                    selectedStudentId = null;
                    originalGrade = null;
                    fetchStudentsByGrade(grade);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving student: " + e.getMessage());
                    Toast.makeText(this, "Error saving student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    interface StaffFetchCallback {
        void onStaffFetched(Map<String, String> driver, Map<String, String> attendant);
    }
}