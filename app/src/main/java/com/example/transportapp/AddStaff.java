package com.example.transportapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddStaff extends AppCompatActivity {

    private EditText staffNameEditText, staffRoleEditText;
    private Button addStaffButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        staffNameEditText = findViewById(R.id.staffNameEditText);
        staffRoleEditText = findViewById(R.id.staffRoleEditText);
        addStaffButton = findViewById(R.id.addStaffButton);

        addStaffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addStaff();
            }
        });
    }

    private void addStaff() {
        String staffName = staffNameEditText.getText().toString().trim();
        String staffRole = staffRoleEditText.getText().toString().trim();

        if (TextUtils.isEmpty(staffName) || TextUtils.isEmpty(staffRole)) {
            Toast.makeText(AddStaff.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> staffData = new HashMap<>();
        staffData.put("name", staffName);
        staffData.put("role", staffRole);

        // Add staff to Firestore
        firestore.collection("staff").add(staffData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(AddStaff.this, "Staff added successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after successful addition
                } else {
                    Toast.makeText(AddStaff.this, "Failed to add staff", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
