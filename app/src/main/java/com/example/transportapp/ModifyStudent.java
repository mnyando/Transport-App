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
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, parentEmailInput, homeLocationInput;
    private Button saveStudentButton;
    private RecyclerView studentRecyclerView;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private String selectedStudentId = null;
    private String originalGrade = null;
    /** Grade whose students are being loaded; older responses for another grade are ignored. */
    private String loadingGrade = null;
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
        parentEmailInput = findViewById(R.id.parentEmailInput);
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
        db.collection(FirestorePaths.ROUTES)
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
                    UserErrors.show(this, "Could not load routes", e);
                });
    }

    private void fetchStudentsByGrade(String grade) {
        if (grade == null || grade.isEmpty()) {
            Log.w(TAG, "fetchStudentsByGrade: Grade is null or empty, aborting fetch");
            Toast.makeText(this, "Invalid grade selected", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "fetchStudentsByGrade: Fetching students for grade: " + grade);
        loadingGrade = grade;
        db.collection(FirestorePaths.STUDENTS).document(grade)
                .collection(FirestorePaths.STUDENT_LIST)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!grade.equals(loadingGrade)) return; // a newer grade was picked meanwhile
                    studentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        // The grade is the parent document, and many records don't repeat it as a
                        // field. Without this the class dropdown fell back to the first grade and
                        // saving moved the child into it.
                        student.setGrade(grade);
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
                    UserErrors.show(this, "Could not load students", e);
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
        parentEmailInput.setText(student.getParentEmail());
        homeLocationInput.setText(student.getHomeLocation());

        // Class selection
        int classPosition = positionOf(classDropdown, student.getGrade());
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
        if (routeList.isEmpty()) {
            // Routes are still loading; don't silently pick the first one on save.
            Toast.makeText(this, "Routes are still loading, try again in a moment", Toast.LENGTH_SHORT).show();
            selectedStudentId = null;
            return;
        }
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

    /** Index of {@code value} in a spinner's adapter, or -1. Works for CharSequence adapters. */
    private static int positionOf(Spinner spinner, String value) {
        if (value == null || spinner.getAdapter() == null) return -1;
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            Object item = spinner.getAdapter().getItem(i);
            if (item != null && value.equals(item.toString())) return i;
        }
        return -1;
    }

    private void clearForm() {
        selectedStudentId = null;
        originalGrade = null;
        studentNameInput.setText("");
        studentIdInput.setText("");
        parentNameInput.setText("");
        parentContactInput.setText("");
        parentEmailInput.setText("");
        homeLocationInput.setText("");
    }

    private void checkAndSaveStudentData() {
        Log.d(TAG, "checkAndSaveStudentData: Starting validation");
        // This screen edits an existing record. Without a selection StudentRecords.save would
        // create a brand-new student instead.
        if (selectedStudentId == null) {
            Toast.makeText(this, R.string.error_select_student_first, Toast.LENGTH_SHORT).show();
            return;
        }
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

        String parentEmail = ParentEmails.normalize(parentEmailInput.getText().toString());
        if (!ParentEmails.isValid(parentEmail)) {
            parentEmailInput.setError("Enter the email the parent will sign in with");
            parentEmailInput.requestFocus();
            return;
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("studentId", studentId);
        fields.put("parentName", parentName);
        fields.put("parentContact", parentContact);
        // The parent's login email is what links their account to this child.
        fields.put("parentEmail", parentEmail);
        fields.put("homeLocation", homeLocation);

        // Editing keeps the record's ID and trip fields; a grade change moves the full record.
        // Capacity is only checked when the child joins a different bus (review #13).
        String fromGrade = originalGrade != null && !originalGrade.isEmpty() ? originalGrade
                : filterDropdown.getSelectedItem() != null ? filterDropdown.getSelectedItem().toString() : grade;
        StudentRecords.Existing existing = new StudentRecords.Existing(fromGrade, selectedStudentId);
        saveStudentButton.setEnabled(false);
        StudentRecords.save(this, db, fields, grade, route, existing, new StudentRecords.Result() {
            @Override
            public void onSaved(String vehicle) {
                saveStudentButton.setEnabled(true);
                Toast.makeText(ModifyStudent.this, R.string.student_updated, Toast.LENGTH_SHORT).show();
                clearForm();
                // Reload the grade on screen (the child may have moved to another grade).
                Object shown = filterDropdown.getSelectedItem();
                fetchStudentsByGrade(shown != null ? shown.toString() : grade);
            }

            @Override
            public void onFailed(String message, Exception e) {
                saveStudentButton.setEnabled(true);
                StudentRecords.showFailure(ModifyStudent.this, message, e);
            }
        });
    }
}
