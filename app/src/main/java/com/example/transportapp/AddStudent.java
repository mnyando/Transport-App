package com.example.transportapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStudent extends AppCompatActivity {

    private static final String TAG = "AddStudent";
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, parentEmailInput, homeLocationInput;
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
        parentEmailInput = findViewById(R.id.parentEmailInput);
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
                    if (routeList.isEmpty()) {
                        Toast.makeText(this, "No routes found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    UserErrors.show(this, "Could not load routes", e);
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

        String parentEmail = ParentEmails.normalize(parentEmailInput.getText().toString());
        if (!ParentEmails.isValid(parentEmail)) {
            parentEmailInput.setError("Enter the email the parent will sign in with");
            parentEmailInput.requestFocus();
            return;
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", studentName);
        fields.put("studentId", studentId);
        fields.put("parentName", parentName);
        fields.put("parentContact", parentContact);
        // The parent's login email is what links their account to this child (see ParentLanding
        // and firestore.rules). The typed parent name is only for display.
        fields.put("parentEmail", parentEmail);
        fields.put("homeLocation", homeLocation);

        // Capacity check, duplicate-ID check and save in one transaction (review #13).
        StudentRecords.save(this, db, fields, studentClass, route, null, new StudentRecords.Result() {
            @Override
            public void onSaved(String vehicle) {
                Toast.makeText(AddStudent.this, R.string.student_added, Toast.LENGTH_SHORT).show();
                clearForm();
            }

            @Override
            public void onFailed(String message, Exception e) {
                StudentRecords.showFailure(AddStudent.this, message, e);
            }
        });
    }

    private void clearForm() {
        studentNameInput.setText("");
        studentIdInput.setText("");
        parentNameInput.setText("");
        parentContactInput.setText("");
        parentEmailInput.setText("");
        homeLocationInput.setText("");
        studentClassInput.setSelection(0);
        routeDropdown.setSelection(0);
    }

}