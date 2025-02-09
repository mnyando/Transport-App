package com.example.transportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class DriverLanding extends AppCompatActivity {

    private RecyclerView tripRecyclerView;
    private RecyclerView notificationRecyclerView;
    private Button chatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_landing);

        // Initialize views
        tripRecyclerView = findViewById(R.id.tripRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        chatButton = findViewById(R.id.chatButton);

        // Set up RecyclerView (Assuming you have an adapter and data, this is a placeholder)
        setupRecyclerViews();

        // Set OnClickListener for the Chat Button
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DriverLanding.this, "Chat with Parents clicked!", Toast.LENGTH_SHORT).show();
                // Add code to open the chat activity
            }
        });
    }

    private void setupRecyclerViews() {
        // TODO: Set up adapters and data for the RecyclerViews
        // Example: tripRecyclerView.setAdapter(new TripAdapter(data));
        // You will need to implement adapters to manage the data in these RecyclerViews
    }
}
