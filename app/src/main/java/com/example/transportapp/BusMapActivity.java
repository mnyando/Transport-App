package com.example.transportapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

    private MapView map;
    private Marker busMarker;
    private TextView status;
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
        status.setText(R.string.bus_map_waiting);
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
                        return;
                    }
                    show(doc);
                });
    }

    private void show(DocumentSnapshot doc) {
        Double lat = doc != null ? doc.getDouble("lat") : null;
        Double lng = doc != null ? doc.getDouble("lng") : null;
        boolean active = doc != null && Boolean.TRUE.equals(doc.getBoolean("active"));
        if (!active || lat == null || lng == null) {
            map.getOverlays().remove(busMarker);
            map.invalidate();
            status.setText(getString(R.string.bus_map_not_on_trip, vehicle));
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
        super.onStop();
    }
}
