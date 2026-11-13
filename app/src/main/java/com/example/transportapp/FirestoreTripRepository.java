package com.example.transportapp;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Firestore implementation of {@link TripRepository}. Paths come from {@link FirestorePaths}. */
public class FirestoreTripRepository implements TripRepository {

    private static final String TAG = "TripRepository";

    private final FirebaseFirestore db;

    public FirestoreTripRepository(FirebaseFirestore db) {
        this.db = db;
    }

    private DocumentReference studentRef(Student s) {
        return db.collection(FirestorePaths.STUDENTS).document(s.getGrade())
                .collection(FirestorePaths.STUDENT_LIST).document(s.getId());
    }

    @Override
    public void loadStudents(String vehicle, Callback<List<Student>> callback) {
        db.collectionGroup(FirestorePaths.STUDENT_LIST)
                .whereEqualTo("vehicle", vehicle)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Student> students = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        try {
                            Student s = doc.toObject(Student.class);
                            s.setId(doc.getId());
                            // students/{grade}/studentList/{id}
                            String[] path = doc.getReference().getPath().split("/");
                            if (path.length >= 2) s.setGrade(path[1]);
                            students.add(s);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Skipping a student record that could not be read", e);
                        }
                    }
                    callback.onResult(students);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void findOpenTrip(String vehicle, String driverId, Callback<OpenTrip> callback) {
        // driverId must be in the query: the rules only let a driver read their own trips.
        db.collection(FirestorePaths.TRIPS)
                .whereEqualTo("vehicle", vehicle)
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("completed", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    long now = System.currentTimeMillis();
                    // Keep the newest recent trip; close everything else (old screens opened a
                    // new trip every time and never closed it).
                    DocumentSnapshot newest = null;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Long start = doc.getLong("startTimeMillis");
                        boolean stale = start == null || now - start > STALE_TRIP_MILLIS;
                        if (stale) {
                            abandon(doc);
                        } else if (newest == null || start > newest.getLong("startTimeMillis")) {
                            if (newest != null) abandon(newest);
                            newest = doc;
                        } else {
                            abandon(doc);
                        }
                    }
                    if (newest == null) {
                        callback.onResult(null);
                        return;
                    }
                    // Trips from the old pick-up screen have no tripType.
                    String type = DROPOFF.equals(newest.getString("tripType")) ? DROPOFF : PICKUP;
                    callback.onResult(new OpenTrip(newest.getId(), type, newest.getLong("startTimeMillis")));
                })
                .addOnFailureListener(callback::onError);
    }

    /** Closes a trip that was never finished (e.g. opened by the old screens and left). */
    private void abandon(DocumentSnapshot doc) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", true);
        updates.put("abandoned", true);
        doc.getReference().update(updates)
                .addOnFailureListener(e -> Log.w(TAG, "Could not close an abandoned trip", e));
    }

    @Override
    public void loadStatuses(String tripId, String tripType, Callback<Map<String, Boolean>> callback) {
        String sub = PICKUP.equals(tripType) ? FirestorePaths.PICKUP_STATUS : FirestorePaths.DROPOFF_STATUS;
        db.collection(FirestorePaths.TRIPS).document(tripId).collection(sub)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Boolean> statuses = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Boolean status = doc.getBoolean("status");
                        if (status != null) statuses.put(doc.getId(), status);
                    }
                    callback.onResult(statuses);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void createTrip(TripDetails details, long startMillis, int studentCount, Callback<String> callback) {
        Map<String, Object> trip = new HashMap<>();
        trip.put("tripType", details.tripType);
        trip.put("vehicle", details.vehicle);
        trip.put("driverId", details.driverId);
        trip.put("driverName", details.driverName);
        trip.put("route", details.route);
        trip.put("startTimeMillis", startMillis);
        trip.put("startTimeFormatted", formatTime(startMillis));
        trip.put("completed", false);
        trip.put("studentCount", studentCount);
        db.collection(FirestorePaths.TRIPS).add(trip)
                .addOnSuccessListener(ref -> callback.onResult(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void joinTrip(String tripId, String vehicle, List<Student> students) {
        for (Student s : students) {
            if (s.getGrade() == null || s.getId() == null) continue;
            studentRef(s).update("currentTripId", tripId)
                    .addOnFailureListener(e -> Log.w(TAG, "Could not mark a student as on the trip", e));
        }
        BusGroups.syncParents(db, vehicle, students);
    }

    @Override
    public int sendAlerts(TripDetails details, String tripId, List<Student> students,
                          Map<String, Boolean> statuses) {
        TripAlerts.Summary summary = TripAlerts.sendTripAlerts(db, students,
                s -> s.getId() == null ? null : statuses.get(s.getId()),
                details.isPickup(), tripId, details.vehicle, details.driverId, details.driverName);
        return summary.skippedNoParentEmail;
    }

    @Override
    public void recordStatus(String tripId, String tripType, Student student, boolean done, long now) {
        if (student.getGrade() == null || student.getId() == null) {
            Log.e(TAG, "Student record is missing its grade or ID; status not saved");
            return;
        }
        boolean pickup = PICKUP.equals(tripType);
        Map<String, Object> status = new HashMap<>();
        status.put(pickup ? "isPicked" : "isDropped", done);
        status.put("timestamp", now);
        status.put("tripId", tripId);
        status.put("type", tripType);

        Map<String, Object> updates = new HashMap<>();
        if (pickup) {
            updates.put("pickupStatus", status);
            if (done) updates.put("pickupTime", formatTime(now));
            updates.put("currentTripId", tripId);
        } else {
            updates.put("dropoffStatus", status);
            if (done) {
                updates.put("dropOffTime", formatTime(now));
                updates.put("currentTripId", null);
            }
            if (student.getPickupStatus() != null) {
                updates.put("lastPickupTripId", student.getPickupStatus().getTripId());
            }
        }
        studentRef(student).update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Could not save the status on the student", e));

        Map<String, Object> tripStatus = new HashMap<>();
        tripStatus.put("studentId", student.getId());
        tripStatus.put("studentName", student.getName());
        tripStatus.put("grade", student.getGrade());
        tripStatus.put("status", done);
        tripStatus.put("timestamp", now);
        db.collection(FirestorePaths.TRIPS).document(tripId)
                .collection(pickup ? FirestorePaths.PICKUP_STATUS : FirestorePaths.DROPOFF_STATUS)
                .document(student.getId())
                .set(tripStatus)
                .addOnFailureListener(e -> Log.e(TAG, "Could not save the status on the trip", e));
    }

    @Override
    public void completeTrip(String tripId, @Nullable Long startMillis, long endMillis,
                             List<String> carriedIds, List<Student> leaveTrip, Callback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("endTimeMillis", endMillis);
        updates.put("endTimeFormatted", formatTime(endMillis));
        updates.put("completed", true);
        updates.put("studentIds", carriedIds);
        if (startMillis != null) {
            updates.put("durationMillis", endMillis - startMillis);
            updates.put("durationFormatted", formatDuration(endMillis - startMillis));
        }
        db.collection(FirestorePaths.TRIPS).document(tripId).update(updates)
                .addOnSuccessListener(v -> {
                    for (Student s : leaveTrip) {
                        if (s.getGrade() == null || s.getId() == null) continue;
                        studentRef(s).update("currentTripId", null)
                                .addOnFailureListener(e -> Log.w(TAG, "Could not clear a student's trip", e));
                    }
                    callback.onResult(null);
                })
                .addOnFailureListener(callback::onError);
    }

    static String formatTime(long millis) {
        return new SimpleDateFormat("EEE, MMM d yyyy 'at' hh:mm a", Locale.getDefault())
                .format(new Date(millis));
    }

    static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours > 0
                ? String.format(Locale.getDefault(), "%d hrs %02d mins", hours, minutes)
                : String.format(Locale.getDefault(), "%d mins", minutes);
    }
}
