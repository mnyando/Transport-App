package com.example.transportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parent dashboard.
 *
 * A parent sees only the students whose {@code parentEmail} equals the verified email of the
 * signed-in Firebase user, and only the notifications addressed to that email. The typed parent
 * name is never used for access, and the email comes from FirebaseAuth rather than an Intent
 * extra, so it cannot be spoofed by another screen. firestore.rules enforces the same check on
 * the server.
 */
public class ParentLanding extends AppCompatActivity {

    private static final String TAG = "ParentLanding";
    private static final int MAX_NOTIFICATIONS = 50;

    private RecyclerView childRecyclerView, notificationRecyclerView;
    private List<Student> childList;
    private List<Notification> notificationList;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;
    private FirebaseFirestore db;
    private String parentId, parentEmail;
    private ListenerRegistration notificationListener;
    private TextView childEmptyText, notificationEmptyText, busStatusText;
    private Button whereIsBusButton, messageBusButton;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this,
                            "Notifications are off. You will only see pick-up and drop-off alerts while this screen is open.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_landing);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No signed-in user; closing parent dashboard");
            finish();
            return;
        }
        if (!user.isEmailVerified()) {
            Toast.makeText(this, "Please verify your email address, then sign in again.", Toast.LENGTH_LONG).show();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        parentId = user.getUid();
        parentEmail = ParentEmails.normalize(user.getEmail());
        if (parentEmail.isEmpty()) {
            Toast.makeText(this, "Your account has no email address. Contact the school.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        childRecyclerView = findViewById(R.id.childRecyclerView);
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        childList = new ArrayList<>();
        notificationList = new ArrayList<>();
        childEmptyText = findViewById(R.id.childEmptyText);
        notificationEmptyText = findViewById(R.id.notificationEmptyText);
        busStatusText = findViewById(R.id.busStatusText);
        whereIsBusButton = findViewById(R.id.whereIsBusButton);
        messageBusButton = findViewById(R.id.messageBusButton);
        whereIsBusButton.setOnClickListener(v -> pickVehicle(this::showBusLocation));
        messageBusButton.setOnClickListener(v -> pickVehicle(this::openBusChat));

        setupRecyclerViews();
        fetchChildrenFromFirestore();
        fetchNotificationsFromFirestore();

        // Make sure this phone can receive push alerts for this parent.
        TransportMessagingService.ensureChannel(this);
        PushTokens.registerCurrentDevice();
        askForNotificationPermission();
    }

    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void fetchChildrenFromFirestore() {
        childList.clear();

        db.collectionGroup("studentList")
                .whereEqualTo("parentEmail", parentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());

                        // Extract grade from document path: students/{grade}/studentList/{studentId}
                        String grade = document.getReference().getParent().getParent().getId();
                        student.setGrade(grade);

                        childList.add(student);
                    }

                    childAdapter.updateList(childList);
                    Log.d(TAG, "Students linked to this parent: " + childList.size());

                    if (childList.isEmpty()) {
                        childEmptyText.setText("No children are linked to your email yet. "
                                + "Ask the school to add " + parentEmail + " to your child's record.");
                        childEmptyText.setVisibility(View.VISIBLE);
                    } else {
                        childEmptyText.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching students", e);
                    childEmptyText.setText("Could not load your children. Check your connection and reopen this screen.");
                    childEmptyText.setVisibility(View.VISIBLE);
                });
    }

    private void fetchNotificationsFromFirestore() {
        // Real-time feed of alerts addressed to this parent's email only.
        notificationListener = db.collection(TripAlerts.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("parentEmail", parentEmail)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching notifications", e);
                        return;
                    }
                    if (snapshots == null) return;

                    notificationList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notificationList.add(notification);
                    }
                    // Newest first, capped so the list stays readable.
                    notificationList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    if (notificationList.size() > MAX_NOTIFICATIONS) {
                        notificationList.subList(MAX_NOTIFICATIONS, notificationList.size()).clear();
                    }

                    notificationAdapter.notifyDataSetChanged();
                    notificationEmptyText.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void setupRecyclerViews() {
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this::onNotificationClick);
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    private void onNotificationClick(Notification notification) {
        if (notification.getVehicle() != null && !notification.getVehicle().isEmpty()) {
            openBusChat(notification.getVehicle());
        } else {
            pickVehicle(this::openBusChat);
        }
    }

    private interface VehicleAction { void run(String vehicle); }

    /** Runs the action for the child's bus, asking which bus when children ride different ones. */
    private void pickVehicle(VehicleAction action) {
        Set<String> vehicles = new LinkedHashSet<>();
        for (Student child : childList) {
            if (child.getVehicle() != null && !child.getVehicle().isEmpty()) vehicles.add(child.getVehicle());
        }
        if (vehicles.isEmpty()) {
            Toast.makeText(this, "None of your children has a bus assigned yet.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] options = vehicles.toArray(new String[0]);
        if (options.length == 1) {
            action.run(options[0]);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Which bus?")
                .setItems(options, (d, which) -> action.run(options[which]))
                .show();
    }

    private void openBusChat(String vehicle) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("vehicle", vehicle);
        intent.putExtra("senderRole", "parent");
        startActivity(intent);
    }

    /** Shows the latest shared bus location and opens it in the phone's maps app. */
    private void showBusLocation(String vehicle) {
        busStatusText.setVisibility(View.VISIBLE);
        busStatusText.setText("Checking where bus " + vehicle + " is...");
        db.collection(BusGroups.COLLECTION).document(BusGroups.docId(vehicle)).get()
                .addOnSuccessListener(doc -> {
                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");
                    Boolean active = doc.getBoolean("active");
                    Long at = doc.getLong("updatedAtMillis");
                    if (lat == null || lng == null || !Boolean.TRUE.equals(active)) {
                        busStatusText.setText("Bus " + vehicle + " is not on a trip right now.");
                        return;
                    }
                    long minutes = at == null ? -1 : Math.max(0, (System.currentTimeMillis() - at) / 60000);
                    busStatusText.setText("Bus " + vehicle + " is on a trip. Location updated "
                            + (minutes < 0 ? "recently" : minutes == 0 ? "just now" : minutes + " min ago") + ".");
                    Uri uri = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                            lat, lng, lat, lng, Uri.encode("Bus " + vehicle)));
                    Intent map = new Intent(Intent.ACTION_VIEW, uri);
                    if (map.resolveActivity(getPackageManager()) != null) {
                        startActivity(map);
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US,
                                    "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng))));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(this, "Install a maps app to see the bus on a map.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not read bus location", e);
                    busStatusText.setText("The bus location isn't available yet. It appears once the driver starts a trip.");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
