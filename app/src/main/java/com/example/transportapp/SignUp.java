package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import android.widget.CheckBox;
import com.google.firebase.firestore.FieldValue;

public class SignUp extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvSignIn;
    private ImageView ivTogglePassword;
    private CheckBox cbConsent;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "SignUpDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        cbConsent = findViewById(R.id.cbConsent);
        findViewById(R.id.tvPrivacyNotice).setOnClickListener(v -> PrivacyNotice.show(this));

        PasswordToggle.attach(ivTogglePassword, etPassword);

        // Handle sign-up button click
        btnSignUp.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = ParentEmails.normalize(etEmail.getText().toString());
            String password = etPassword.getText().toString().trim();

            // Input validation
            if (fullName.isEmpty()) {
                etFullName.setError("Full name is required");
                etFullName.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            if (!cbConsent.isChecked()) {
                Toast.makeText(SignUp.this, R.string.consent_required, Toast.LENGTH_LONG).show();
                PrivacyNotice.show(SignUp.this);
                return;
            }

            // Proceed to Firebase sign-up process
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Store user information in Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("fullName", fullName);
                                userData.put("email", email);
                                userData.put("role", "parent");  // Default role
                                // Recorded consent to the privacy notice the user agreed to.
                                userData.put("consentVersion", PrivacyNotice.VERSION);
                                userData.put("consentAt", FieldValue.serverTimestamp());

                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            // Parents are linked to children by their *verified*
                                            // email, so the account must be verified before the
                                            // parent dashboard will open.
                                            user.sendEmailVerification()
                                                    .addOnCompleteListener(verifyTask -> {
                                                        mAuth.signOut();
                                                        Toast.makeText(SignUp.this,
                                                                "Account created. We sent a verification link to "
                                                                        + email + ". Open it, then sign in.",
                                                                Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(SignUp.this, Login.class);
                                                        startActivity(intent);
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save user data to Firestore", e);
                                            Toast.makeText(SignUp.this, "Could not finish creating your account. Please try again.", Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                Log.e(TAG, "User is null after creation");
                                Toast.makeText(SignUp.this, "User creation failed.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Sign-up failed", task.getException());
                            Toast.makeText(SignUp.this, "Sign-up failed. Check your email address, or sign in if you already have an account.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Handle sign-in click to navigate to login
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();  // Close current activity and go back to login
        });
    }
}
