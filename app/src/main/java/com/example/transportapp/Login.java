package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get references to UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        // Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, fetch user role from Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRole(user.getUid());
                        }
                    } else {
                        // If sign-in fails, display a message to the user
                        Log.w("Login", "signInWithEmail:failure", task.getException());
                        Toast.makeText(Login.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            navigateToRoleBasedPage(role);
                        } else {
                            Toast.makeText(Login.this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, "Failed to fetch user role: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToRoleBasedPage(String role) {
        if (role == null) {
            Toast.makeText(this, "User role is undefined", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        switch (role.toLowerCase()) {
            case "school":
                intent = new Intent(Login.this, SchoolLanding.class);
                break;
            case "parent":
                intent = new Intent(Login.this, ParentLanding.class);
                break;
            case "driver":
                intent = new Intent(Login.this, DriverLanding.class);
                break;
            default:
                Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        // Start the appropriate activity and finish the login activity
        startActivity(intent);
        finish();
    }
}

