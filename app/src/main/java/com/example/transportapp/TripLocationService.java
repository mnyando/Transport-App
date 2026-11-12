package com.example.transportapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Shares the bus location while a pick-up or drop-off trip is running.
 *
 * Runs as a foreground service with a visible notification, so the driver always knows the
 * location is being shared, and it stops as soon as the trip is completed or the trip screen
 * closes. Locations go to {@code busGroups/{vehicle}}, which only staff and the parents of
 * children on that bus can read (see firestore.rules).
 */
public class TripLocationService extends Service implements LocationListener {

    private static final String TAG = "TripLocationService";
    private static final String CHANNEL_ID = "trip_location";
    private static final int NOTIFICATION_ID = 4201;
    private static final long MIN_INTERVAL_MS = 15_000;
    private static final float MIN_DISTANCE_M = 20f;

    private static final String EXTRA_VEHICLE = "vehicle";
    private static final String EXTRA_TRIP_ID = "tripId";
    private static final String EXTRA_TRIP_TYPE = "tripType";

    private LocationManager locationManager;
    private String vehicle, tripId, tripType;

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void start(Context context, String vehicle, String tripId, String tripType) {
        if (!hasLocationPermission(context)) return;
        Intent intent = new Intent(context, TripLocationService.class)
                .putExtra(EXTRA_VEHICLE, vehicle)
                .putExtra(EXTRA_TRIP_ID, tripId)
                .putExtra(EXTRA_TRIP_TYPE, tripType);
        ContextCompat.startForegroundService(context, intent);
    }

    /**
     * Stops sharing, marks the bus as no longer on a trip and removes its last position, so the
     * location of the bus is only stored while a trip is running (review section 3). The
     * clearStaleBusLocations Cloud Function does the same if this phone goes offline mid-trip.
     */
    public static void stop(Context context, String vehicle) {
        context.stopService(new Intent(context, TripLocationService.class));
        String id = BusGroups.docId(vehicle);
        if (id.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("active", false);
        data.put("endedAt", FieldValue.serverTimestamp());
        data.put("lat", FieldValue.delete());
        data.put("lng", FieldValue.delete());
        data.put("accuracy", FieldValue.delete());
        FirebaseFirestore.getInstance().collection(BusGroups.COLLECTION).document(id)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not mark trip inactive", e));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        vehicle = intent.getStringExtra(EXTRA_VEHICLE);
        tripId = intent.getStringExtra(EXTRA_TRIP_ID);
        tripType = intent.getStringExtra(EXTRA_TRIP_TYPE);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type);

        if (!hasLocationPermission(this) || BusGroups.docId(vehicle).isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startUpdates();
        return START_NOT_STICKY;
    }

    @SuppressWarnings("MissingPermission") // checked in onStartCommand
    private void startUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;
        locationManager.removeUpdates(this);
        boolean any = false;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            if (locationManager.isProviderEnabled(provider)) {
                try {
                    locationManager.requestLocationUpdates(provider, MIN_INTERVAL_MS, MIN_DISTANCE_M,
                            this, Looper.getMainLooper());
                    Location last = locationManager.getLastKnownLocation(provider);
                    if (last != null) onLocationChanged(last);
                    any = true;
                } catch (SecurityException e) {
                    Log.w(TAG, "Location permission revoked", e);
                }
            }
        }
        if (!any) Log.w(TAG, "No location provider is enabled");
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("vehicle", vehicle);
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("accuracy", location.getAccuracy());
        data.put("tripId", tripId);
        data.put("tripType", tripType);
        data.put("active", true);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection(BusGroups.COLLECTION)
                .document(BusGroups.docId(vehicle))
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not save bus location", e));
    }

    // Older Android versions call these; nothing to do.
    @Override public void onProviderEnabled(@NonNull String provider) {}
    @Override public void onProviderDisabled(@NonNull String provider) {}
    @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Trip location sharing", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shown while the bus location is shared with parents during a trip.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Sharing bus location")
                .setContentText("Parents can see the bus until you complete the trip.")
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
