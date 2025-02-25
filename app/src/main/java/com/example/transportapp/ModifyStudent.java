package com.example.transportapp;

import android.os.Bundle;
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

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ModifyStudent extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference studentsRef;
    private Spinner filterDropdown, classDropdown, routeDropdown;
    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Button saveStudentButton;
    private RecyclerView studentRecyclerView;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private String selectedStudentId = null; // To track editing student

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_student);

        db = FirebaseFirestore.getInstance();
        studentsRef = db.collection("students");

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

        // Setup RecyclerView
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(studentList, this::onStudentClicked);
        studentRecyclerView.setAdapter(studentAdapter);

        // Load filter dropdown from strings.xml
        ArrayAdapter<CharSequence> gradeAdapter = ArrayAdapter.createFromResource(this,
                R.array.grade_levels, android.R.layout.simple_spinner_item);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterDropdown.setAdapter(gradeAdapter);

        // Fetch students when a grade is selected
        filterDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = parent.getItemAtPosition(position).toString();
                fetchStudentsByGrade(selectedGrade);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Save Button Click
        saveStudentButton.setOnClickListener(v -> saveStudentData());
    }

    // Fetch students by selected grade
    private void fetchStudentsByGrade(String grade) {
        studentsRef.whereEqualTo("grade", grade).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        studentList.add(student);
                    }
                    studentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching students", Toast.LENGTH_SHORT).show());
    }

    // When a student is clicked, populate the form for editing
    private void onStudentClicked(Student student) {
        selectedStudentId = student.getId(); // Store ID for updating
        studentNameInput.setText(student.getName());
        studentIdInput.setText(student.getStudentId());
        parentNameInput.setText(student.getParentName());
        parentContactInput.setText(student.getParentContact());
        homeLocationInput.setText(student.getHomeLocation());

        Toast.makeText(this, "Editing " + student.getName() + "'s record", Toast.LENGTH_SHORT).show();
    }

    // Save or update student data
    private void saveStudentData() {
        String name = studentNameInput.getText().toString().trim();
        String studentId = studentIdInput.getText().toString().trim();
        String parentName = parentNameInput.getText().toString().trim();
        String parentContact = parentContactInput.getText().toString().trim();
        String homeLocation = homeLocationInput.getText().toString().trim();
        String grade = filterDropdown.getSelectedItem().toString();

        if (name.isEmpty() || studentId.isEmpty() || parentName.isEmpty() || parentContact.isEmpty() || homeLocation.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = new Student(name, studentId, grade, parentName, parentContact, homeLocation);

        if (selectedStudentId == null) {
            // Add new student
            studentsRef.add(student)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                        fetchStudentsByGrade(grade);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add student", Toast.LENGTH_SHORT).show());
        } else {
            // Update existing student
            DocumentReference studentRef = studentsRef.document(selectedStudentId);
            studentRef.set(student)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Student record updated successfully", Toast.LENGTH_SHORT).show();
                        selectedStudentId = null; // Reset ID after updating
                        fetchStudentsByGrade(grade);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update student", Toast.LENGTH_SHORT).show());
        }
    }
}
