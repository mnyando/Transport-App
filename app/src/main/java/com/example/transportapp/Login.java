package com.example.transportapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private TextView tvSignUp;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Login button click listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Navigate to the SignUp page
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(Login.this, SignUp.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            Log.w(TAG, "❌ Login failed: Email is empty");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            Log.w(TAG, "❌ Login failed: Password is empty");
            return;
        }

        Log.d(TAG, "🔑 Attempting to login with email: " + email);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "✅ Login successful: " + user.getEmail());
                            fetchUserRole(user.getUid(), user.getEmail());
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }

    private void handleAuthError(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "❌ Authentication failed: No account found.");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "❌ Authentication failed: Invalid credentials.");
        } else {
            Toast.makeText(this, "Authentication failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "❌ Authentication failed: " + exception.getMessage(), exception);
        }
    }

    private void fetchUserRole(String authUid, String email) {
        Log.d(TAG, "📡 Fetching user role for UID: " + authUid);

        db.collection("users").document(authUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        Log.d(TAG, "✅ User role fetched: " + role);
                        if ("driver".equalsIgnoreCase(role)) {
                            fetchDriverId(authUid, email);
                        } else if ("parent".equalsIgnoreCase(role)) {
                            fetchParentId(authUid, email);
                        } else {
                            navigateToRoleBasedPage(role, null, email);
                        }
                    } else {
                        Log.w(TAG, "⚠️ User data not found for UID: " + authUid);
                        Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch user role: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to fetch user role.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchParentId(String authUid, String email) {
        Log.d(TAG, "📡 Fetching parent profile for UID: " + authUid);

        db.collection("users").document(authUid).get()
                .addOnSuccessListener(parentDoc -> {
                    if (parentDoc.exists()) {
                        String parentId = parentDoc.getId();
                        Log.d(TAG, "✅ Parent found: ID = " + parentId);
                        navigateToRoleBasedPage("parent", parentId, email);
                    } else {
                        Log.w(TAG, "⚠️ Parent profile not found for UID: " + authUid);
                        Toast.makeText(Login.this, "Parent profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch parent data: " + e.getMessage(), e);
                    Toast.makeText(Login.this, "Failed to fetch parent data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchDriverId(String authUid, String email) {
        Log.d(TAG, "📡 Fetching driver profile for UID: " + authUid);

        db.collection("staff").whereEqualTo("authUid", authUid).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String driverId = document.getId();
                        String staffName = document.getString("staffName");
                        String assignedVehicle = document.getString("assignedVehicle");
                        String assignedRoute = document.getString("assignedRoute");

                        Log.d(TAG, "✅ Driver found: ID = " + driverId);

                        Intent intent = new Intent(Login.this, DriverLanding.class);
                        intent.putExtra("driverId", driverId);
                        intent.putExtra("driverAuthUid", authUid);
                        intent.putExtra("driverEmail", email);
                        intent.putExtra("driverName", staffName);
                        intent.putExtra("assignedVehicle", assignedVehicle);
                        intent.putExtra("assignedRoute", assignedRoute);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "⚠️ Driver profile not found for UID: " + authUid);
                        Toast.makeText(Login.this, "Driver profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch driver data: " + e.getMessage(), e);
                    Toast.makeText(Login.this, "Failed to fetch driver data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToRoleBasedPage(String role, String id, String email) {
        if (role == null) {
            Log.e(TAG, "❌ User role is undefined");
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
                intent.putExtra("parentId", id);
                intent.putExtra("parentEmail", email);
                break;
            default:
                Log.w(TAG, "⚠️ Unknown role: " + role);
                Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                return;
        }
        Log.d(TAG, "🚀 Navigating to " + role + " landing page.");
        startActivity(intent);
        finish();
    }
}
