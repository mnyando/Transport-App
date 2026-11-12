package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Role picker shown after the landing screen (formerly {@code Navigation}, renamed so it is not
 * confused with the AndroidX Navigation library).
 *
 * <p>The buttons only help people find the sign-in screen. The screen a user ends up on is
 * decided by the role stored on their server-side profile ({@code users/{uid}.role}) in
 * {@link Login}, and Firestore rules enforce it, so picking a different button grants nothing.
 */
public class RoleSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        findViewById(R.id.school_button).setOnClickListener(v -> navigateToLogin("school"));
        findViewById(R.id.parent_button).setOnClickListener(v -> navigateToLogin("parent"));
        findViewById(R.id.driver_button).setOnClickListener(v -> navigateToLogin("driver"));
    }

    /** Opens sign-in. The role is only a hint; Login uses the role on the user's profile. */
    private void navigateToLogin(String role) {
        Intent intent = new Intent(this, Login.class);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}
