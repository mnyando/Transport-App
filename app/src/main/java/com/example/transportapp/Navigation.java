package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Navigation extends AppCompatActivity {

    private Button schoolButton, parentButton, driverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation); // Ensure this matches your XML layout file name

        // Find buttons by their IDs
        schoolButton = findViewById(R.id.school_button);
        parentButton = findViewById(R.id.parent_button);
        driverButton = findViewById(R.id.driver_button);

        // Set click listeners for each button
        schoolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("school");
            }
        });

        parentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("parent");
            }
        });

        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("driver");
            }
        });
    }

    /**
     * Navigates to the login page and passes the selected role as an extra
     * @param role The role selected by the user
     */
    private void navigateToLogin(String role) {
        Intent intent = new Intent(Navigation.this, Login.class);
        intent.putExtra("role", role); // Pass the role to the Login activity
        startActivity(intent);
    }
}
