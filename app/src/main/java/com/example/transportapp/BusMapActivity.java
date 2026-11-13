package com.example.transportapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

/**
 * Live map of the child's bus for parents (review #10), using OpenStreetMap through osmdroid.
 * It follows busGroups/{bus}, which the driver's phone updates every 15 s / 20 m during a trip
 * (TripLocationService) and which is cleared when the trip ends. Only parents of children on
 * that bus can read it (firestore.rules).
 *
 * OpenStreetMap's free tile servers are for light use. If the app grows, switch to a paid tile
 * provider by changing {@link #TILES}.
 */
public class BusMapActivity extends AppCompatActivity {

    private static final String EXTRA_VEHICLE = "vehicle";
    private static final org.osmdroid.tileprovider.tilesource.ITileSource TILES = TileSourceFactory.MAPNIK;
    private static final double BUS_ZOOM = 16.0;
    /** A position older than this is shown as "Signal delayed" instead of "Live". */
    private static final long DELAYED_AFTER_MINUTES = 5;

    /** What the live-state pill on the floating card shows. */
    private enum LiveState { LOCATING, LIVE, DELAYED, IDLE, OFFLINE }

    private MapView map;
    private Marker busMarker;
    private TextView status;
    private TextView driver;
    private TextView pillText;
    private View pill;
    private View liveDot;
    private ImageView busIcon;
    private View recentre;
    private ObjectAnimator pulse;
    private String vehicle;
    private ListenerRegistration listener;
    private boolean centred;

    static Intent intent(Context context, String vehicle) {
        return new Intent(context, BusMapActivity.class).putExtra(EXTRA_VEHICLE, vehicle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vehicle = getIntent().getStringExtra(EXTRA_VEHICLE);
        if (vehicle == null || vehicle.isEmpty()) {
            finish();
            return;
        }

        // osmdroid identifies the app to the tile server and caches tiles in app storage only
        // (no storage permission needed).
        IConfigurationProvider config = Configuration.getInstance();
        config.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        config.setUserAgentValue(getPackageName());
        File base = new File(getCacheDir(), "osmdroid");
        config.setOsmdroidBasePath(base);
        config.setOsmdroidTileCache(new File(base, "tiles"));

        setTitle(getString(R.string.bus_map_title, vehicle));
        setContentView(R.layout.activity_bus_map);
        status = findViewById(R.id.busMapStatus);
        map = findViewById(R.id.busMap);
        map.setTileSource(TILES);
        map.setMultiTouchControls(true);
        map.getController().setZoom(BUS_ZOOM);

        busMarker = new Marker(map);
        busMarker.setTitle(getString(R.string.bus_map_marker, vehicle));
        busMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_bus_marker));
        busMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        ((TextView) findViewById(R.id.busMapTitle)).setText(getString(R.string.bus_map_title, vehicle));
        driver = findViewById(R.id.busMapDriver);
        pill = findViewById(R.id.busMapPill);
        pillText = findViewById(R.id.busMapPillText);
        liveDot = findViewById(R.id.busMapLiveDot);
        busIcon = findViewById(R.id.busMapIcon);
        recentre = findViewById(R.id.busMapRecentreButton);
        recentre.setOnClickListener(v -> map.getController().animateTo(busMarker.getPosition()));
        findViewById(R.id.busMapMessageButton).setOnClickListener(v -> startActivity(
                new Intent(this, ChatActivity.class)
                        .putExtra("vehicle", vehicle)
                        .putExtra("senderRole", "parent")));

        // Keep the bus in the visible part of the map, above the floating card.
        View card = findViewById(R.id.busMapCard);
        card.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int covered = map.getBottom() - v.getTop();
            if (covered > 0 && map.getMapCenterOffsetY() != -covered / 2) {
                map.setMapCenterOffset(0, -covered / 2);
            }
        });

        status.setText(R.string.bus_map_waiting);
        setLiveState(LiveState.LOCATING);
    }

    /** Colours the pill and bus icon for the state; the dot pulses only while live. */
    private void setLiveState(LiveState state) {
        int container, on, label;
        switch (state) {
            case LIVE:
                container = R.color.status_info_container; on = R.color.status_on_info;
                label = R.string.bus_map_state_live; break;
            case DELAYED:
                container = R.color.status_warning_container; on = R.color.status_on_warning;
                label = R.string.bus_map_state_delayed; break;
            case IDLE:
                container = R.color.surface_variant; on = R.color.text_secondary;
                label = R.string.bus_map_state_idle; break;
            case OFFLINE:
                container = R.color.status_error_container; on = R.color.status_on_error;
                label = R.string.bus_map_state_offline; break;
            default:
                container = R.color.status_info_container; on = R.color.status_on_info;
                label = R.string.bus_map_state_locating; break;
        }
        ColorStateList bg = ColorStateList.valueOf(ContextCompat.getColor(this, container));
        ColorStateList fg = ColorStateList.valueOf(ContextCompat.getColor(this, on));
        pill.setBackgroundTintList(bg);
        busIcon.setBackgroundTintList(bg);
        busIcon.setImageTintList(fg);
        liveDot.setBackgroundTintList(fg);
        pillText.setTextColor(fg);
        pillText.setText(label);
        recentre.setEnabled(state == LiveState.LIVE || state == LiveState.DELAYED);

        boolean animate = state == LiveState.LIVE && animationsEnabled();
        if (animate && pulse == null) {
            pulse = ObjectAnimator.ofFloat(liveDot, View.ALPHA, 1f, 0.25f);
            pulse.setDuration(900);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.start();
        } else if (!animate && pulse != null) {
            pulse.cancel();
            pulse = null;
            liveDot.setAlpha(1f);
        }
    }

    /** Respects the system "Remove animations" / animator-scale setting. */
    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (map == null) return;
        listener = FirebaseFirestore.getInstance().collection(FirestorePaths.BUS_GROUPS)
                .document(BusGroups.docId(vehicle))
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        status.setText(R.string.bus_map_unavailable);
                        setLiveState(LiveState.OFFLINE);
                        return;
                    }
                    show(doc);
                });
    }

    private void show(DocumentSnapshot doc) {
        Double lat = doc != null ? doc.getDouble("lat") : null;
        Double lng = doc != null ? doc.getDouble("lng") : null;
        boolean active = doc != null && Boolean.TRUE.equals(doc.getBoolean("active"));
        String driverName = doc != null ? doc.getString("driverName") : null;
        boolean hasDriver = driverName != null && !driverName.isEmpty();
        driver.setVisibility(hasDriver ? View.VISIBLE : View.GONE);
        if (hasDriver) driver.setText(getString(R.string.bus_map_driver, driverName));
        if (!active || lat == null || lng == null) {
            map.getOverlays().remove(busMarker);
            map.invalidate();
            status.setText(getString(R.string.bus_map_not_on_trip, vehicle));
            setLiveState(LiveState.IDLE);
            return;
        }
        GeoPoint point = new GeoPoint(lat, lng);
        busMarker.setPosition(point);
        if (!map.getOverlays().contains(busMarker)) map.getOverlays().add(busMarker);
        if (!centred) {
            map.getController().setCenter(point);
            centred = true;
        } else {
            map.getController().animateTo(point);
        }
        map.invalidate();

        Long updated = doc.getLong("updatedAtMillis");
        long minutes = updated == null ? -1 : Math.max(0, (System.currentTimeMillis() - updated) / 60000);
        status.setText(minutes < 0 ? getString(R.string.bus_map_live, vehicle)
                : minutes == 0 ? getString(R.string.bus_map_updated_now, vehicle)
                : getResources().getQuantityString(R.plurals.bus_map_updated_minutes, (int) minutes, vehicle, (int) minutes));
        setLiveState(minutes >= DELAYED_AFTER_MINUTES ? LiveState.DELAYED : LiveState.LIVE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        super.onStop();
    }
}
