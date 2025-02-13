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

public class AddRoute extends AppCompatActivity {

    private EditText routeNameEditText, routeNumberEditText;
    private Button addRouteButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        routeNameEditText = findViewById(R.id.routeNameEditText);
        routeNumberEditText = findViewById(R.id.routeNumberEditText);
        addRouteButton = findViewById(R.id.addRouteButton);

        addRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRoute();
            }
        });
    }

    private void addRoute() {
        String routeName = routeNameEditText.getText().toString().trim();
        String routeNumber = routeNumberEditText.getText().toString().trim();

        if (TextUtils.isEmpty(routeName) || TextUtils.isEmpty(routeNumber)) {
            Toast.makeText(AddRoute.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> routeData = new HashMap<>();
        routeData.put("name", routeName);
        routeData.put("number", routeNumber);

        // Add route to Firestore
        firestore.collection("routes").add(routeData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(AddRoute.this, "Route added successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after successful addition
                } else {
                    Toast.makeText(AddRoute.this, "Failed to add route", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
