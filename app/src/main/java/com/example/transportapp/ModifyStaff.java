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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ModifyStaff extends AppCompatActivity {

    private EditText modifyStaffName, modifyStaffRole;
    private Button updateStaffButton;
    private FirebaseFirestore firestore;
    private String staffId;  // Assume this is passed when opening the activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        modifyStaffName = findViewById(R.id.modifyStaffName);
        modifyStaffRole = findViewById(R.id.modifyStaffRole);
        updateStaffButton = findViewById(R.id.updateStaffButton);

        // Assume staffId was passed through an Intent
        staffId = getIntent().getStringExtra("staffId");

        updateStaffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStaff();
            }
        });
    }

    private void updateStaff() {
        String newName = modifyStaffName.getText().toString().trim();
        String newRole = modifyStaffRole.getText().toString().trim();

        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newRole)) {
            Toast.makeText(ModifyStaffActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", newName);
        updatedData.put("role", newRole);

        DocumentReference staffRef = firestore.collection("staff").document(staffId);
        staffRef.update(updatedData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ModifyStaffActivity.this, "Staff updated successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after successful update
                } else {
                    Toast.makeText(ModifyStaffActivity.this, "Failed to update staff", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
