package com.example.transportapp;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Data access for a driver's pick-up or drop-off run (review #17: UI -> ViewModel -> Repository).
 * Keeps {@link TripViewModel} free of Firebase so the trip rules can be unit tested.
 * {@link FirestoreTripRepository} is the real implementation.
 */
public interface TripRepository {

    String PICKUP = "pickup";
    String DROPOFF = "dropoff";

    interface Callback<T> {
        void onResult(T value);

        void onError(Exception e);
    }

    /** Children assigned to this vehicle. */
    void loadStudents(String vehicle, Callback<List<Student>> callback);

    /** This driver's unfinished trip on this vehicle, or {@code null} (review #12: one at a time). */
    void findOpenTrip(String vehicle, String driverId, Callback<OpenTrip> callback);

    /** What was already recorded on a trip, by student ID (TRUE done, FALSE not done). */
    void loadStatuses(String tripId, String tripType, Callback<Map<String, Boolean>> callback);

    /** Creates the trip document; returns its ID. */
    void createTrip(TripDetails details, long startMillis, int studentCount, Callback<String> callback);

    /** Saves one child's status on the trip and on the student record. */
    void recordStatus(String tripId, String tripType, Student student, boolean done, long nowMillis);

    /**
     * Closes the trip. {@code carriedIds} are the children marked done (saved as
     * {@code studentIds}, used for attendance and trip history); {@code leaveTrip} are the
     * children whose {@code currentTripId} is cleared.
     */
    void completeTrip(String tripId, @Nullable Long startMillis, long endMillis,
                      List<String> carriedIds, List<Student> leaveTrip, Callback<Void> callback);

    /** Marks the children as on this trip ({@code currentTripId}) and refreshes the bus chat members. */
    void joinTrip(String tripId, String vehicle, List<Student> students);

    /**
     * Tells each parent what was recorded for their child (and the school about any child not
     * confirmed). Returns how many children have no parent email on file.
     */
    int sendAlerts(TripDetails details, String tripId, List<Student> students, Map<String, Boolean> statuses);

    /** Open trips older than this are treated as abandoned, not resumed. */
    long STALE_TRIP_MILLIS = 12L * 60 * 60 * 1000;

    /** Who is driving which bus, passed from the driver's home screen. */
    final class TripDetails {
        public final String tripType;
        public final String vehicle;
        public final String driverId;
        @Nullable public final String driverName;
        @Nullable public final String route;

        public TripDetails(String tripType, String vehicle, String driverId,
                           @Nullable String driverName, @Nullable String route) {
            this.tripType = tripType;
            this.vehicle = vehicle;
            this.driverId = driverId;
            this.driverName = driverName;
            this.route = route;
        }

        public boolean isPickup() {
            return PICKUP.equals(tripType);
        }
    }

    final class OpenTrip {
        public final String tripId;
        public final String tripType;
        @Nullable public final Long startMillis;

        public OpenTrip(String tripId, String tripType, @Nullable Long startMillis) {
            this.tripId = tripId;
            this.tripType = tripType;
            this.startMillis = startMillis;
        }
    }
}
