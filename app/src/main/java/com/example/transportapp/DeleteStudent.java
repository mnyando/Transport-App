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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Removes a student (review #7).
 *
 * The school picks a grade, then the child, checks the details and confirms. The record is
 * archived rather than destroyed: one batch copies it to {@code archivedStudents/{id}} and deletes
 * it from {@code students/{grade}/studentList/{id}}, so it disappears from every live screen but
 * the school can still look it up, and trip history (which stores the student's ID and name)
 * stays meaningful.
 */
public class DeleteStudent extends AppCompatActivity {

    private static final String TAG = "DeleteStudent";
    static final String ARCHIVE = "archivedStudents";

    private EditText studentNameInput, studentIdInput, parentNameInput, parentContactInput, homeLocationInput;
    private Spinner filterDropdown, classDropdown, routeDropdown;
    private Button deleteButton;
    private FirebaseFirestore db;

    private final List<Student> students = new ArrayList<>();
    private StudentAdapter studentAdapter;
    private Student selected;
    private String selectedGrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_student);
        db = FirebaseFirestore.getInstance();

        studentNameInput = findViewById(R.id.studentNameInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        parentNameInput = findViewById(R.id.parentNameInput);
        parentContactInput = findViewById(R.id.parentContactInput);
        homeLocationInput = findViewById(R.id.homeLocationInput);
        filterDropdown = findViewById(R.id.filterDropdown);
        classDropdown = findViewById(R.id.classDropdown);
        routeDropdown = findViewById(R.id.routeDropdown);
        deleteButton = findViewById(R.id.deleteStudentButton);

        // This screen only shows the details; edits are made on Modify Student.
        for (View v : new View[]{studentNameInput, studentIdInput, parentNameInput,
                parentContactInput, homeLocationInput, classDropdown, routeDropdown}) {
            v.setEnabled(false);
        }

        RecyclerView list = findViewById(R.id.studentRecyclerView);
        studentAdapter = new StudentAdapter(students, this::showStudent);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(studentAdapter);

        ArrayAdapter<CharSequence> grades = ArrayAdapter.createFromResource(this,
                R.array.grade_levels, android.R.layout.simple_spinner_item);
        grades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterDropdown.setAdapter(grades);
        filterDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStudents(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        deleteButton.setEnabled(false);
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadStudents(String grade) {
        selectedGrade = grade;
        showStudent(null);
        db.collection(FirestorePaths.STUDENTS).document(grade)
                .collection(FirestorePaths.STUDENT_LIST)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!grade.equals(selectedGrade)) return; // a newer grade was picked meanwhile
                    students.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Student s = doc.toObject(Student.class);
                        s.setId(doc.getId());
                        s.setGrade(grade);
                        students.add(s);
                    }
                    studentAdapter.notifyDataSetChanged();
                    if (students.isEmpty()) {
                        Toast.makeText(this, getString(R.string.no_students_in_grade, grade),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> UserErrors.show(this, "Could not load students", e));
    }

    private void showStudent(Student student) {
        selected = student;
        deleteButton.setEnabled(student != null);
        studentNameInput.setText(student != null ? student.getName() : "");
        studentIdInput.setText(student != null ? student.getStudentId() : "");
        parentNameInput.setText(student != null ? student.getParentName() : "");
        parentContactInput.setText(student != null ? student.getParentContact() : "");
        homeLocationInput.setText(student != null ? student.getHomeLocation() : "");
    }

    private void confirmDelete() {
        if (selected == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_student_title)
                .setMessage(getString(R.string.delete_student_confirm, selected.getName()))
                .setPositiveButton(R.string.delete, (d, w) -> archive(selected))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void archive(Student student) {
        DocumentReference live = db.collection(FirestorePaths.STUDENTS).document(student.getGrade())
                .collection(FirestorePaths.STUDENT_LIST).document(student.getId());
        deleteButton.setEnabled(false);

        // Read the full document (not just the fields the Student class knows) so nothing is lost.
        live.get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, R.string.student_already_removed, Toast.LENGTH_LONG).show();
                        loadStudents(student.getGrade());
                        return;
                    }
                    writeArchive(live, doc, student);
                })
                .addOnFailureListener(e -> {
                    deleteButton.setEnabled(true);
                    UserErrors.show(this, "Could not delete student", e);
                });
    }

    private void writeArchive(DocumentReference live, DocumentSnapshot doc, Student student) {
        Map<String, Object> data = doc.getData() != null ? new HashMap<>(doc.getData()) : new HashMap<>();
        data.put("grade", student.getGrade());
        data.put("archivedAt", FieldValue.serverTimestamp());
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) data.put("archivedBy", uid);

        WriteBatch batch = db.batch();
        batch.set(db.collection(ARCHIVE).document(student.getId()), data);
        batch.delete(live);
        batch.commit()
                .addOnSuccessListener(v -> {
                    Log.i(TAG, "Student archived");
                    Toast.makeText(this, R.string.student_deleted, Toast.LENGTH_SHORT).show();
                    loadStudents(student.getGrade());
                })
                .addOnFailureListener(e -> {
                    deleteButton.setEnabled(true);
                    UserErrors.show(this, "Could not delete student", e);
                });
    }
}
