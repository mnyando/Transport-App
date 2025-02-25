package com.example.transportapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteStudent extends AppCompatActivity {
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Spinner classDropdown, routeDropdown;
    private Button  deleteButton;
    private FirebaseFirestore db;
    private String studentId; // Firestore Document ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_student);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data passed from previous activity
        studentId = getIntent().getStringExtra("studentId");

        // Initialize UI elements
        studentNameInput = findViewById(R.id.studentNameInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        parentNameInput = findViewById(R.id.parentNameInput);
        parentContactInput = findViewById(R.id.parentContactInput);
        homeLocationInput = findViewById(R.id.homeLocationInput);
        classDropdown = findViewById(R.id.classDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        deleteButton = findViewById(R.id.deleteStudentButton);

        // Populate the fields with the received data
        studentNameInput.setText(getIntent().getStringExtra("studentName"));
        studentIdInput.setText(getIntent().getStringExtra("studentIdNumber"));
        parentNameInput.setText(getIntent().getStringExtra("parentName"));
        parentContactInput.setText(getIntent().getStringExtra("parentContact"));
        homeLocationInput.setText(getIntent().getStringExtra("homeLocation"));

        // Delete student record from Firestore
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void updateStudentData() {
        DocumentReference studentRef = db.collection("students").document(studentId);

        studentRef.update(
                "studentName", studentNameInput.getText().toString(),
                "studentIdNumber", studentIdInput.getText().toString(),
                "parentName", parentNameInput.getText().toString(),
                "parentContact", parentContactInput.getText().toString(),
                "homeLocation", homeLocationInput.getText().toString()
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(DeleteStudent.this, "Student record updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(DeleteStudent.this, "Error updating record!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student Record")
                .setMessage("Are you sure you want to delete this student?")
                .setPositiveButton("Yes", (dialog, which) -> deleteStudentRecord())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStudentRecord() {
        DocumentReference studentRef = db.collection("students").document(studentId);

        studentRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(DeleteStudent.this, "Student record deleted successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity after deletion
            } else {
                Toast.makeText(DeleteStudent.this, "Error deleting record!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
