package com.example.transportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentLanding extends AppCompatActivity {

    private static final String TAG = "ParentLanding";

    private RecyclerView childRecyclerView;
    private RecyclerView notificationRecyclerView;
    private Button chatButton;
    private ArrayList<String> childList;
    private ArrayList<String> notificationList;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerViews and Button
        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        chatButton = findViewById(R.id.chatButton);

        // Initialize child and notification data
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();

        // Fetch children and notifications from Firestore
        fetchChildrenFromFirestore();
        fetchNotificationsFromFirestore();

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

    private void fetchChildrenFromFirestore() {
        CollectionReference childrenRef = db.collection("children");
        childrenRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        String childData = document.getString("name") + " - " + document.getString("grade");
                        childList.add(childData);
                    }
                    childAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Error getting children data: ", task.getException());
                }
            }
        });
    }

    private void fetchNotificationsFromFirestore() {
        CollectionReference notificationsRef = db.collection("notifications");
        notificationsRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        String notification = document.getString("message");
                        notificationList.add(notification);
                    }
                    notificationAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Error getting notifications: ", task.getException());
                }
            }
        });
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
