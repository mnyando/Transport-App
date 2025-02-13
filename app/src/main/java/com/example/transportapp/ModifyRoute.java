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

public class ModifyRoute extends AppCompatActivity {

    private EditText modifyRouteName, modifyRouteNumber;
    private Button updateRouteButton;
    private FirebaseFirestore firestore;
    private String routeId;  // Assume this is passed when opening the activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_route);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        modifyRouteName = findViewById(R.id.modifyRouteName);
        modifyRouteNumber = findViewById(R.id.modifyRouteNumber);
        updateRouteButton = findViewById(R.id.updateRouteButton);

        // Assume routeId was passed through an Intent
        routeId = getIntent().getStringExtra("routeId");

        updateRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRoute();
            }
        });
    }

    private void updateRoute() {
        String newName = modifyRouteName.getText().toString().trim();
        String newNumber = modifyRouteNumber.getText().toString().trim();

        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newNumber)) {
            Toast.makeText(ModifyRoute.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", newName);
        updatedData.put("number", newNumber);

        DocumentReference routeRef = firestore.collection("routes").document(routeId);
        routeRef.update(updatedData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ModifyRoute.this, "Route updated successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after successful update
                } else {
                    Toast.makeText(ModifyRoute.this, "Failed to update route", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
