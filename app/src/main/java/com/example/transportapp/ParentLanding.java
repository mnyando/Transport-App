package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ParentLanding extends AppCompatActivity {

    private RecyclerView childRecyclerView;
    private RecyclerView notificationRecyclerView;
    private Button chatButton;
    private ArrayList<String> childList;
    private ArrayList<String> notificationList;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        // Initialize RecyclerViews and Button
        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        chatButton = findViewById(R.id.chatButton);

        // Initialize child and notification data
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();

        // Sample data for children and notifications
        loadSampleData();

        // Set up the RecyclerViews with adapters
        setupRecyclerViews();

        // Set up chat button click listener
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to chat activity
                Intent intent = new Intent(ParentLanding.this, ChatActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadSampleData() {
        // Add sample children data
        childList.add("John Doe - Grade 4");
        childList.add("Jane Doe - Grade 2");

        // Add sample notifications data
        notificationList.add("Bus will arrive at 8:00 AM.");
        notificationList.add("School is closed on Friday.");
    }

    private void setupRecyclerViews() {
        // Set up child RecyclerView
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);

        // Set up notification RecyclerView
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList);
        notificationRecyclerView.setAdapter(notificationAdapter);
    }
}
