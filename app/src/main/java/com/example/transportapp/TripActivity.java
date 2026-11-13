package com.example.transportapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * The driver's pick-up or drop-off run (review #12, #17, #21). One screen for both kinds of
 * trip; the rules live in {@link TripViewModel}.
 */
public class TripActivity extends AppCompatActivity implements TripChecklistAdapter.Host {

    private static final String EXTRA_TYPE = "tripType";

    private TripViewModel viewModel;
    private TripChecklistAdapter adapter;
    private TextView statusText, emptyText;
    private View emptyState;
    private ProgressBar progress;
    private MaterialButton actionButton;
    private boolean pickup;
    private String vehicle;
    private boolean locationSharing;

    /** Leaving mid-trip asks first; the trip stays open and can be finished later. */
    private final OnBackPressedCallback leaveActiveTrip = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            new AlertDialog.Builder(TripActivity.this)
                    .setTitle(R.string.trip_leave_title)
                    .setMessage(R.string.trip_leave_message)
                    .setPositiveButton(R.string.trip_leave, (d, w) -> {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    })
                    .setNegativeButton(R.string.trip_stay, null)
                    .show();
        }
    };

    static Intent intent(Context context, String tripType, String vehicle, String driverId,
                         @Nullable String driverName, @Nullable String route) {
        return new Intent(context, TripActivity.class)
                .putExtra(EXTRA_TYPE, tripType)
                .putExtra("assignedVehicle", vehicle)
                .putExtra("driverId", driverId)
                .putExtra("driverName", driverName)
                .putExtra("assignedRoute", route);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = getIntent();
        pickup = !TripRepository.DROPOFF.equals(in.getStringExtra(EXTRA_TYPE));
        vehicle = in.getStringExtra("assignedVehicle");
        String driverId = in.getStringExtra("driverId");
        setTitle(pickup ? R.string.title_pick_up : R.string.title_drop_off);
        setContentView(R.layout.activity_trip);

        if (vehicle == null || vehicle.isEmpty() || driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, R.string.trip_missing_driver, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        statusText = findViewById(R.id.tripStatusText);
        emptyState = findViewById(R.id.tripEmptyState);
        emptyText = findViewById(R.id.tripEmptyText);
        progress = findViewById(R.id.tripProgress);
        actionButton = findViewById(R.id.tripActionButton);

        adapter = new TripChecklistAdapter(this, pickup);
        RecyclerView list = findViewById(R.id.studentListRecyclerView);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        getOnBackPressedDispatcher().addCallback(this, leaveActiveTrip);

        viewModel = new ViewModelProvider(this, TripViewModel.factory(
                new FirestoreTripRepository(FirebaseFirestore.getInstance()))).get(TripViewModel.class);
        TripRepository.TripDetails details = new TripRepository.TripDetails(
                pickup ? TripRepository.PICKUP : TripRepository.DROPOFF, vehicle, driverId,
                in.getStringExtra("driverName"), in.getStringExtra("assignedRoute"));
        findViewById(R.id.tripRetryButton).setOnClickListener(v -> viewModel.load(details));

        viewModel.getState().observe(this, this::render);
        viewModel.getError().observe(this, e -> {
            if (e == null) return;
            UserErrors.show(this, getString(R.string.trip_error), e);
            viewModel.clearError();
        });
        viewModel.load(details);
    }

    private void render(TripViewModel.TripState s) {
        boolean showList = s.phase == TripViewModel.Phase.READY || s.phase == TripViewModel.Phase.STARTING
                || s.phase == TripViewModel.Phase.ACTIVE || s.phase == TripViewModel.Phase.COMPLETING;
        progress.setVisibility(s.phase == TripViewModel.Phase.LOADING ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(s.phase == TripViewModel.Phase.LOAD_FAILED
                || s.phase == TripViewModel.Phase.NO_STUDENTS
                || s.phase == TripViewModel.Phase.OTHER_TRIP_OPEN ? View.VISIBLE : View.GONE);
        findViewById(R.id.tripRetryButton).setVisibility(
                s.phase == TripViewModel.Phase.LOAD_FAILED ? View.VISIBLE : View.GONE);
        adapter.submit(showList ? s.students : java.util.Collections.emptyList());
        leaveActiveTrip.setEnabled(s.phase == TripViewModel.Phase.ACTIVE);

        actionButton.setVisibility(View.VISIBLE);
        actionButton.setEnabled(true);
        actionButton.setOnClickListener(null);
        switch (s.phase) {
            case LOADING:
                statusText.setText(R.string.trip_loading);
                actionButton.setVisibility(View.GONE);
                break;
            case LOAD_FAILED:
                statusText.setText(R.string.trip_load_failed);
                emptyText.setText(R.string.trip_load_failed_detail);
                actionButton.setVisibility(View.GONE);
                break;
            case NO_STUDENTS:
                statusText.setText(getString(R.string.trip_bus, vehicle));
                emptyText.setText(R.string.trip_no_students);
                actionButton.setVisibility(View.GONE);
                break;
            case OTHER_TRIP_OPEN:
                renderOtherTripOpen(s);
                break;
            case READY:
            case STARTING:
                statusText.setText(getString(R.string.trip_not_started, s.students.size(), vehicle));
                actionButton.setText(R.string.start_trip);
                actionButton.setEnabled(s.phase == TripViewModel.Phase.READY);
                actionButton.setOnClickListener(v -> viewModel.startTrip());
                break;
            case ACTIVE:
            case COMPLETING:
                updateActiveStatus(s);
                if (s.resumed && savedResumeToastShown()) {
                    Toast.makeText(this, R.string.trip_resumed, Toast.LENGTH_LONG).show();
                }
                actionButton.setText(R.string.trip_complete);
                actionButton.setEnabled(s.phase == TripViewModel.Phase.ACTIVE);
                actionButton.setOnClickListener(v -> confirmComplete());
                if (s.phase == TripViewModel.Phase.ACTIVE) startLocationSharing();
                break;
            case COMPLETED:
                stopLocationSharing();
                Toast.makeText(this, R.string.trip_completed, Toast.LENGTH_LONG).show();
                if (s.parentsWithoutEmail > 0) {
                    Toast.makeText(this, getResources().getQuantityString(R.plurals.trip_parents_without_email,
                            s.parentsWithoutEmail, s.parentsWithoutEmail), Toast.LENGTH_LONG).show();
                }
                finish();
                break;
        }
    }

    private boolean resumeToastShown;

    /** The "continuing your trip" message is shown once per screen, not on every redraw. */
    private boolean savedResumeToastShown() {
        if (resumeToastShown) return false;
        resumeToastShown = true;
        return true;
    }

    private void updateActiveStatus(TripViewModel.TripState s) {
        int marked = s.students.size() - viewModel.unmarkedCount();
        String started = s.startMillis != null ? FirestoreTripRepository.formatTime(s.startMillis) : "";
        statusText.setText(getString(R.string.trip_active_status, started, marked, s.students.size()));
    }

    private void renderOtherTripOpen(TripViewModel.TripState s) {
        TripRepository.OpenTrip other = s.otherTrip;
        boolean otherIsPickup = other != null && TripRepository.PICKUP.equals(other.tripType);
        String otherName = getString(otherIsPickup ? R.string.trip_kind_pickup : R.string.trip_kind_dropoff);
        statusText.setText(R.string.trip_other_open_title);
        emptyText.setText(getString(R.string.trip_other_open, otherName,
                other != null && other.startMillis != null ? FirestoreTripRepository.formatTime(other.startMillis) : ""));
        actionButton.setText(getString(R.string.trip_open_other, otherName));
        actionButton.setOnClickListener(v -> {
            TripRepository.TripDetails d = viewModel.getDetails();
            if (d == null || other == null) return;
            startActivity(intent(this, other.tripType, d.vehicle, d.driverId, d.driverName, d.route));
            finish();
        });
    }

    private void confirmComplete() {
        int unmarked = viewModel.unmarkedCount();
        if (unmarked == 0) {
            viewModel.completeTrip();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.trip_unmarked_title)
                .setMessage(getResources().getQuantityString(pickup
                        ? R.plurals.trip_unmarked_pickup : R.plurals.trip_unmarked_dropoff, unmarked, unmarked))
                .setPositiveButton(R.string.trip_complete, (d, w) -> viewModel.completeTrip())
                .setNegativeButton(R.string.trip_go_back, null)
                .show();
    }

    // ---------- checklist ----------

    @Nullable
    @Override
    public Boolean statusFor(Student student) {
        return viewModel.statusFor(student);
    }

    @Override
    public boolean canMark() {
        return viewModel.canMark();
    }

    @Override
    public void onStatusChosen(Student student, @Nullable Boolean status) {
        viewModel.setStatus(student, status);
        TripViewModel.TripState s = viewModel.getState().getValue();
        if (s != null && s.phase == TripViewModel.Phase.ACTIVE) updateActiveStatus(s);
    }

    // ---------- live bus location ----------

    private final ActivityResultLauncher<String[]> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (TripLocationService.hasLocationPermission(this)) {
                    startLocationSharing();
                } else {
                    Toast.makeText(this, R.string.trip_location_off, Toast.LENGTH_LONG).show();
                }
            });

    private boolean askedForLocation;

    /** Shares the bus position with parents while this trip runs and this screen is open. */
    private void startLocationSharing() {
        TripViewModel.TripState s = viewModel.getState().getValue();
        if (locationSharing || s == null || s.tripId == null) return;
        if (!TripLocationService.hasLocationPermission(this)) {
            if (askedForLocation) return;
            askedForLocation = true;
            locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        TripLocationService.start(this, vehicle, s.tripId, pickup ? TripRepository.PICKUP : TripRepository.DROPOFF);
        locationSharing = true;
    }

    private void stopLocationSharing() {
        if (!locationSharing) return;
        locationSharing = false;
        TripLocationService.stop(this, vehicle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Rotation keeps sharing; leaving the screen stops it (the trip itself stays open).
        if (!isChangingConfigurations()) stopLocationSharing();
    }
}
