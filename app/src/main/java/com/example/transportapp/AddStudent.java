package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddStudent extends AppCompatActivity {

    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Spinner routeDropdown, studentClassInput;
    private Button saveStudentButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        db = FirebaseFirestore.getInstance();

        studentNameInput = findViewById(R.id.studentNameInput);
        studentClassInput = findViewById(R.id.classDropdown);
        studentIdInput = findViewById(R.id.studentIdInput);
        parentNameInput = findViewById(R.id.parentNameInput);
        parentContactInput = findViewById(R.id.parentContactInput);
        homeLocationInput = findViewById(R.id.homeLocationInput);
        routeDropdown = findViewById(R.id.routeDropdown);
        saveStudentButton = findViewById(R.id.saveStudentButton);

        saveStudentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudentData();
            }
        });
    }

    private void saveStudentData() {
        String studentName = studentNameInput.getText().toString().trim();
        String studentClass = studentClassInput.getSelectedItem().toString();
        String studentId = studentIdInput.getText().toString().trim();
        String parentName = parentNameInput.getText().toString().trim();
        String parentContact = parentContactInput.getText().toString().trim();
        String homeLocation = homeLocationInput.getText().toString().trim();
        String route = routeDropdown.getSelectedItem().toString();

        if (studentName.isEmpty() || studentClass.isEmpty() || studentId.isEmpty() || parentName.isEmpty() || parentContact.isEmpty() || homeLocation.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

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
                .addOnSuccessListener(aVoid -> Toast.makeText(AddStudent.this, "Student added successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(AddStudent.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
