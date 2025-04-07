package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUp.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRole(user.getUid(), email);
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Login failed", task.getException());
                    }
                });
    }

    private void fetchUserRole(String authUid, String email) {
        Log.d(TAG, "Fetching user role for UID: " + authUid);

        db.collection("users").document(authUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        String fullName = document.getString("fullName");
                        Log.d(TAG, "User role fetched: " + role);

                        if ("driver".equalsIgnoreCase(role)) {
                            if (fullName != null && !fullName.isEmpty()) {
                                findDriverInStaffCollection(fullName, email);
                            } else {
                                Toast.makeText(this, "Driver name not found in profile", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                Log.w(TAG, "Driver name missing in user profile");
                            }
                        } else if ("parent".equalsIgnoreCase(role)) {
                            fetchParentId(authUid, email);
                        } else {
                            navigateToRoleBasedPage(role, authUid, email);
                        }
                    } else {
                        Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "User data not found for UID: " + authUid);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user role", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch user role", e);
                });
    }

    private void findDriverInStaffCollection(String fullName, String email) {
        Log.d(TAG, "Searching staff records for: " + fullName);

        db.collection("staff")
                .whereEqualTo("staffName", fullName)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot staffDoc = task.getResult().getDocuments().get(0);
                        processDriverProfile(staffDoc, email);
                    } else {
                        Toast.makeText(this,
                                "No staff record found for: " + fullName,
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        Log.w(TAG, "No staff record found for driver: " + fullName);
                    }
                });
    }

    private void processDriverProfile(DocumentSnapshot staffDoc, String email) {
        String driverId = staffDoc.getId();
        String staffName = staffDoc.getString("staffName");
        String assignedVehicle = staffDoc.getString("assignedVehicle");
        String assignedRoute = staffDoc.getString("assignedRoute");

        if (staffName == null || assignedVehicle == null || assignedRoute == null) {
            Toast.makeText(this,
                    "Incomplete driver profile. Contact administrator.",
                    Toast.LENGTH_LONG).show();
            mAuth.signOut();
            Log.w(TAG, "Incomplete driver profile for: " + staffName);
            return;
        }

        Intent intent = new Intent(this, DriverLanding.class);
        intent.putExtra("driverId", driverId);
        intent.putExtra("driverEmail", email);
        intent.putExtra("driverName", staffName);
        intent.putExtra("assignedVehicle", assignedVehicle);
        intent.putExtra("assignedRoute", assignedRoute);

        // Optional fields
        if (staffDoc.contains("licenseNumber")) {
            intent.putExtra("licenseNumber", staffDoc.getString("licenseNumber"));
        }
        if (staffDoc.contains("phoneNumber")) {
            intent.putExtra("phoneNumber", staffDoc.getString("phoneNumber"));
        }

        startActivity(intent);
        finish();
        Log.d(TAG, "Driver logged in successfully: " + staffName);
    }

    private void fetchParentId(String authUid, String email) {
        Log.d(TAG, "Fetching parent profile for UID: " + authUid);

        db.collection("users").document(authUid).get()
                .addOnSuccessListener(parentDoc -> {
                    if (parentDoc.exists()) {
                        String parentId = parentDoc.getId();
                        Log.d(TAG, "Parent found: ID = " + parentId);
                        navigateToRoleBasedPage("parent", parentId, email);
                    } else {
                        Toast.makeText(this, "Parent profile not found!", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Parent profile not found for UID: " + authUid);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch parent data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch parent data", e);
                });
    }

    private void navigateToRoleBasedPage(String role, String id, String email) {
        Intent intent;
        switch (role.toLowerCase()) {
            case "parent":
                intent = new Intent(this, ParentLanding.class);
                intent.putExtra("parentId", id);
                intent.putExtra("parentEmail", email);
                break;
            case "school":
                intent = new Intent(this, SchoolLanding.class);
                break;
            default:
                Toast.makeText(this, "Unauthorized access for role: " + role, Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                Log.w(TAG, "Unauthorized role access attempt: " + role);
                return;
        }
        startActivity(intent);
        finish();
        Log.d(TAG, "Navigating to " + role + " landing page");
    }
}