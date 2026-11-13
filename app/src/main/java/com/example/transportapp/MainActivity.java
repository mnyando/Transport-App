package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already signed in: go straight to this account's home screen (review section 5).
        if (savedInstanceState == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(Login.resumeSession(this));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        findViewById(R.id.get_started).setOnClickListener(v ->
                startActivity(new Intent(this, RoleSelectActivity.class)));
    }
}
