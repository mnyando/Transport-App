package com.example.transportapp;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Data access for the parent dashboard (review section 4: UI -> ViewModel -> Repository).
 *
 * <p>The interface keeps {@link ParentDashboardViewModel} free of Firebase so it can be unit
 * tested with a fake. {@link FirestoreParentRepository} is the real implementation. Firestore's
 * own offline cache (on by default on Android) means a parent still sees the last known
 * children and alerts without a connection.
 */
public interface ParentRepository {

    interface Callback<T> {
        void onResult(T value);

        void onError(Exception e);
    }

    /** Something that can be stopped, such as a live listener. */
    interface Subscription {
        void cancel();
    }

    /** The children whose record holds this parent's verified email. */
    void loadChildren(String parentEmail, Callback<List<Student>> callback);

    /** Live feed of alerts addressed to this parent's email. */
    Subscription listenToNotifications(String parentEmail, Callback<List<Notification>> callback);

    /** The latest shared position of a bus; {@code null} location fields when it is not on a trip. */
    void loadBusLocation(String vehicle, Callback<BusLocation> callback);

    /** Snapshot of busGroups/{vehicle}. */
    final class BusLocation {
        @Nullable public final Double lat;
        @Nullable public final Double lng;
        public final boolean active;
        @Nullable public final Long updatedAtMillis;

        public BusLocation(@Nullable Double lat, @Nullable Double lng, boolean active,
                           @Nullable Long updatedAtMillis) {
            this.lat = lat;
            this.lng = lng;
            this.active = active;
            this.updatedAtMillis = updatedAtMillis;
        }

        public boolean isOnTrip() {
            return active && lat != null && lng != null;
        }
    }
}
