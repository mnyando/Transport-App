package com.example.transportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parent dashboard.
 *
 * A parent sees only the students whose {@code parentEmail} equals the verified email of the
 * signed-in Firebase user, and only the notifications addressed to that email. The typed parent
 * name is never used for access, and the email comes from FirebaseAuth rather than an Intent
 * extra, so it cannot be spoofed by another screen. firestore.rules enforces the same check on
 * the server.
 *
 * Data loading lives in {@link ParentDashboardViewModel} and {@link FirestoreParentRepository};
 * this activity only draws the state and handles taps.
 */
public class ParentLanding extends AppCompatActivity {

    private static final String TAG = "ParentLanding";

    private final List<Student> childList = new ArrayList<>();
    private SwipeRefreshLayout refreshLayout;
    private ChildAdapter childAdapter;
    private NotificationAdapter notificationAdapter;
    private ParentDashboardViewModel viewModel;
    private String parentEmail;
    private TextView childEmptyText, notificationEmptyText, busStatusText;

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
        parentEmail = ParentEmails.normalize(user.getEmail());
        if (parentEmail.isEmpty()) {
            Toast.makeText(this, "Your account has no email address. Contact the school.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Session.setUpHomeToolbar(this, null);
        findViewById(R.id.alertSettingsButton).setOnClickListener(v -> AlertPreferences.showDialog(this));

        childEmptyText = findViewById(R.id.childEmptyText);
        notificationEmptyText = findViewById(R.id.notificationEmptyText);
        busStatusText = findViewById(R.id.busStatusText);
        findViewById(R.id.whereIsBusButton).setOnClickListener(v -> pickVehicle(this::checkBus));
        findViewById(R.id.messageBusButton).setOnClickListener(v -> pickVehicle(this::openBusChat));
        setupRecyclerViews();
        // Pull down to reload the children (review #20).
        refreshLayout = findViewById(R.id.parentRefresh);
        refreshLayout.setColorSchemeResources(R.color.brand_primary);
        refreshLayout.setOnRefreshListener(() -> viewModel.refreshChildren());

        viewModel = new ViewModelProvider(this, ParentDashboardViewModel.factory(
                new FirestoreParentRepository(FirebaseFirestore.getInstance())))
                .get(ParentDashboardViewModel.class);
        viewModel.getChildren().observe(this, this::showChildren);
        viewModel.getNotifications().observe(this, this::showNotifications);
        viewModel.getNotificationsFailed().observe(this, failed -> {
            if (Boolean.TRUE.equals(failed)) Log.e(TAG, "Error fetching notifications");
        });
        viewModel.getBusStatus().observe(this, this::showBusStatus);
        viewModel.start(parentEmail);

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

    private void setupRecyclerViews() {
        RecyclerView childRecyclerView = findViewById(R.id.childRecyclerView);
        childRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childRecyclerView.setAdapter(childAdapter);

        RecyclerView notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(this::onNotificationClick);
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    private void showChildren(ParentDashboardViewModel.ChildrenState state) {
        if (state.loading) {
            if (!refreshLayout.isRefreshing()) childEmptyText.setText(R.string.loading_your_children);
            return;
        }
        refreshLayout.setRefreshing(false);
        childList.clear();
        childList.addAll(state.children);
        childAdapter.updateList(childList);
        Log.d(TAG, "Students linked to this parent: " + childList.size());

        if (state.failed) {
            childEmptyText.setText(R.string.children_load_failed);
            childEmptyText.setVisibility(View.VISIBLE);
        } else if (childList.isEmpty()) {
            childEmptyText.setText(getString(R.string.no_children_linked, parentEmail));
            childEmptyText.setVisibility(View.VISIBLE);
        } else {
            childEmptyText.setVisibility(View.GONE);
        }
    }

    private void showNotifications(List<Notification> notifications) {
        notificationAdapter.submitList(new ArrayList<>(notifications));
        notificationEmptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
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
        List<String> vehicles = viewModel.getVehicles();
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

    private void checkBus(String vehicle) {
        viewModel.checkBus(vehicle);
    }

    /** Shows the latest shared bus location and opens the live map once (review #10). */
    private void showBusStatus(ParentDashboardViewModel.BusStatus status) {
        busStatusText.setVisibility(View.VISIBLE);
        String vehicle = status.vehicle;
        switch (status.kind) {
            case CHECKING:
                busStatusText.setText(getString(R.string.bus_checking, vehicle));
                return;
            case NOT_ON_TRIP:
                busStatusText.setText(getString(R.string.bus_map_not_on_trip, vehicle));
                return;
            case UNAVAILABLE:
                busStatusText.setText(R.string.bus_location_unavailable);
                return;
            case ON_TRIP:
            default:
                break;
        }
        long minutes = status.minutesAgo;
        busStatusText.setText(minutes < 0 ? getString(R.string.bus_map_live, vehicle)
                : minutes == 0 ? getString(R.string.bus_map_updated_now, vehicle)
                : getResources().getQuantityString(R.plurals.bus_map_updated_minutes, (int) minutes, vehicle, (int) minutes));
        if (status.takeOpenMap()) startActivity(BusMapActivity.intent(this, vehicle));
    }
}
