package com.example.transportapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Sign-in, and the router that sends each role to its own home screen.
 *
 * Firebase keeps the user signed in between launches, so when the app opens with a saved session
 * (see {@link #resumeSession}) this screen skips the form and routes straight to the home screen
 * for that account's role (review section 5: "send each role straight to its own home screen and
 * remember it"). The role is always re-read from the server, never cached on the phone.
 */
public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String EXTRA_RESUME = "resumeSession";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progress;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /** Opens the home screen for the account that is already signed in. */
    static Intent resumeSession(Context context) {
        return new Intent(context, Login.class).putExtra(EXTRA_RESUME, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.loginProgress);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUp.class)));
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
        PasswordToggle.attach(findViewById(R.id.ivTogglePassword), etPassword);

        FirebaseUser current = mAuth.getCurrentUser();
        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_RESUME, false)
                && current != null) {
            setBusy(true);
            fetchUserRole(current.getUid(), current.getEmail());
        }
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!busy);
        etEmail.setEnabled(!busy);
        etPassword.setEnabled(!busy);
    }

    /** Ends the attempt: shows why, signs out if asked, and gives the form back. */
    private void fail(@StringRes int message, boolean signOut) {
        if (signOut) mAuth.signOut();
        setBusy(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required));
            return;
        }

        setBusy(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (task.isSuccessful() && user != null) {
                        fetchUserRole(user.getUid(), email);
                    } else {
                        Log.e(TAG, "Login failed", task.getException());
                        fail(R.string.error_sign_in_failed, false);
                    }
                });
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_reset_needs_email));
            etEmail.requestFocus();
            return;
        }
        // Same message whether or not the account exists, so the form can't be used to find
        // out which emails are registered.
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) Log.w(TAG, "Password reset not sent", t.getException());
            Toast.makeText(this, R.string.password_reset_sent, Toast.LENGTH_LONG).show();
        });
    }

    private void fetchUserRole(String authUid, String email) {
        db.collection("users").document(authUid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.w(TAG, "User data not found");
                        fail(R.string.error_profile_missing, true);
                        return;
                    }
                    String role = document.getString("role");
                    Log.d(TAG, "User role fetched: " + role);

                    if ("driver".equalsIgnoreCase(role)) {
                        // staffId is set on the server (linkStaffAccount) when the school saves
                        // this email on the driver's staff record; it decides which bus they see.
                        String staffId = document.getString("staffId");
                        if (staffId == null || staffId.isEmpty()) {
                            Log.w(TAG, "Driver account has no linked staff record");
                            fail(R.string.driver_not_linked, true);
                        } else {
                            requireVerifiedEmailThen(() -> loadDriverStaffRecord(staffId, email));
                        }
                    } else if ("parent".equalsIgnoreCase(role)) {
                        requireVerifiedEmailThen(() -> openHome(new Intent(this, ParentLanding.class)
                                .putExtra("parentId", authUid)
                                .putExtra("parentEmail", email)));
                    } else if ("school".equalsIgnoreCase(role)) {
                        openHome(new Intent(this, SchoolLanding.class));
                    } else {
                        Log.w(TAG, "Unauthorized role access attempt: " + role);
                        fail(R.string.error_role_not_allowed, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user role", e);
                    // Keep a saved session: this is usually just no connection, and the user can
                    // retry with the button once they are back online.
                    fail(R.string.error_role_lookup_failed, false);
                });
    }

    /**
     * Parents see children whose record holds their email, and drivers are linked to their staff
     * record by email, so we must know the email really belongs to them. Unverified accounts get
     * a fresh link and are signed out.
     */
    private void requireVerifiedEmailThen(Runnable onVerified) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            setBusy(false);
            return;
        }
        user.reload().addOnCompleteListener(r -> {
            FirebaseUser fresh = mAuth.getCurrentUser();
            if (fresh != null && fresh.isEmailVerified()) {
                // The Firestore rules read email_verified from the ID token, which still says
                // "false" until it is refreshed after the user clicks the link.
                fresh.getIdToken(true).addOnCompleteListener(t -> onVerified.run());
                return;
            }
            if (fresh != null) {
                fresh.sendEmailVerification();
            }
            fail(R.string.error_verify_email_first, true);
        });
    }

    private void loadDriverStaffRecord(String staffId, String email) {
        db.collection("staff").document(staffId).get()
                .addOnSuccessListener(staffDoc -> {
                    if (staffDoc.exists()) {
                        processDriverProfile(staffDoc, email);
                    } else {
                        Log.w(TAG, "Linked staff record no longer exists");
                        fail(R.string.driver_not_linked, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Could not load driver staff record", e);
                    fail(R.string.driver_profile_error, true);
                });
    }

    private void processDriverProfile(DocumentSnapshot staffDoc, String email) {
        String staffName = staffDoc.getString("staffName");
        String assignedVehicle = staffDoc.getString("assignedVehicle");
        String assignedRoute = staffDoc.getString("assignedRoute");

        if (staffName == null || assignedVehicle == null || assignedRoute == null) {
            Log.w(TAG, "Incomplete driver profile");
            fail(R.string.error_driver_profile_incomplete, true);
            return;
        }

        Intent intent = new Intent(this, DriverLanding.class);
        intent.putExtra("driverId", staffDoc.getId());
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
        openHome(intent);
    }

    /** The home screen becomes the only screen in the task, so Back leaves the app. */
    private void openHome(Intent home) {
        PushTokens.registerCurrentDevice();
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }
}
