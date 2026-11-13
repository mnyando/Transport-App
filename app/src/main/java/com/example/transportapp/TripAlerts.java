package com.example.transportapp;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends pick-up and drop-off alerts that match what the driver actually recorded for each child.
 *
 * <ul>
 *   <li>Each alert goes only to the parent whose login email is on that student record.</li>
 *   <li>A child marked "picked up" / "dropped off" gives the parent that message.</li>
 *   <li>A child marked "not picked" / "not dropped", or left unmarked, gives the parent a
 *       warning instead, and also raises an alert for the school in {@code schoolAlerts}.</li>
 * </ul>
 *
 * Writing a document to {@code notifications} is what triggers the push message: the
 * {@code sendParentPush} Cloud Function in {@code functions/index.js} sends it to the parent's
 * phones. The in-app feed in ParentLanding reads the same documents.
 */
public final class TripAlerts {

    private static final String TAG = "TripAlerts";

    public static final String COLLECTION_NOTIFICATIONS = FirestorePaths.NOTIFICATIONS;
    public static final String COLLECTION_SCHOOL_ALERTS = FirestorePaths.SCHOOL_ALERTS;

    public static final String STATUS_DONE = "done";
    public static final String STATUS_NOT_DONE = "not_done";
    public static final String STATUS_UNMARKED = "unmarked";

    private TripAlerts() {}

    /** What the driver recorded for one child on this trip. */
    public interface StatusLookup {
        /** TRUE = picked/dropped, FALSE = marked not picked/not dropped, null = not marked. */
        Boolean statusFor(Student student);
    }

    /** Result counts, so the screen can tell the driver what happened. */
    public static final class Summary {
        public int done;
        public int notDone;
        public int unmarked;
        public int skippedNoParentEmail;
    }

    public static Summary sendTripAlerts(FirebaseFirestore db, List<Student> students,
                                         StatusLookup lookup, boolean isPickup, String tripId,
                                         String vehicle, String driverId, String driverName) {
        Summary summary = new Summary();
        for (Student student : students) {
            Boolean status = lookup.statusFor(student);
            String statusKey = status == null ? STATUS_UNMARKED : (status ? STATUS_DONE : STATUS_NOT_DONE);
            if (status == null) summary.unmarked++;
            else if (status) summary.done++;
            else summary.notDone++;

            String childName = student.getName() != null ? student.getName() : "Your child";
            String title = titleFor(isPickup, statusKey);
            String message = messageFor(isPickup, statusKey, childName);
            String type = isPickup ? "pickup" : "dropoff";

            // The school hears about every child who was not confirmed, whether or not a parent
            // email is on file, so no child is left on (or off) the bus unnoticed.
            if (!STATUS_DONE.equals(statusKey)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", type);
                alert.put("status", statusKey);
                alert.put("studentId", student.getId());
                alert.put("studentName", student.getName());
                alert.put("grade", student.getGrade());
                alert.put("tripId", tripId);
                alert.put("vehicle", vehicle);
                alert.put("driverId", driverId);
                alert.put("driverName", driverName);
                alert.put("message", message);
                alert.put("resolved", false);
                alert.put("timestamp", System.currentTimeMillis());
                alert.put("createdAt", FieldValue.serverTimestamp());
                db.collection(COLLECTION_SCHOOL_ALERTS).add(alert)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save school alert", e));
            }

            String parentEmail = ParentEmails.normalize(student.getParentEmail());
            if (parentEmail.isEmpty()) {
                // Old records only have a typed parent name. We never send by name, so the school
                // must add the parent's email on Modify Student before this parent gets alerts.
                summary.skippedNoParentEmail++;
                Log.w(TAG, "Student record has no parent email; parent alert not sent");
                continue;
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("parentEmail", parentEmail);
            notification.put("studentId", student.getId());
            notification.put("studentName", student.getName());
            notification.put("type", type);
            notification.put("status", statusKey);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("tripId", tripId);
            notification.put("vehicle", vehicle);
            notification.put("driverId", driverId);
            notification.put("driverName", driverName);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("createdAt", FieldValue.serverTimestamp());
            db.collection(COLLECTION_NOTIFICATIONS).add(notification)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save parent notification", e));
        }
        return summary;
    }

    /**
     * Emergency from the driver: one urgent school alert, plus a notification (and therefore a
     * push message) to every parent with a child on this bus.
     *
     * @return how many parents were notified
     */
    public static int sendSos(FirebaseFirestore db, List<Student> studentsOnBus, String vehicle,
                              String driverId, String driverName, String details) {
        String message = "Emergency reported on bus " + vehicle
                + (details == null || details.isEmpty() ? "" : ": " + details)
                + ". The school has been alerted and will contact you.";

        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "sos");
        alert.put("status", "emergency");
        alert.put("vehicle", vehicle);
        alert.put("driverId", driverId);
        alert.put("driverName", driverName);
        alert.put("message", "SOS from " + (driverName != null ? driverName : "the driver")
                + " on bus " + vehicle + (details == null || details.isEmpty() ? "" : ": " + details));
        alert.put("resolved", false);
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("createdAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_SCHOOL_ALERTS).add(alert)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save SOS alert", e));

        java.util.Set<String> notified = new java.util.HashSet<>();
        for (Student student : studentsOnBus) {
            String parentEmail = ParentEmails.normalize(student.getParentEmail());
            if (parentEmail.isEmpty() || !notified.add(parentEmail)) continue;
            Map<String, Object> notification = new HashMap<>();
            notification.put("parentEmail", parentEmail);
            notification.put("type", "sos");
            notification.put("status", "emergency");
            notification.put("title", "Emergency on the school bus");
            notification.put("message", message);
            notification.put("vehicle", vehicle);
            notification.put("driverId", driverId);
            notification.put("driverName", driverName);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("createdAt", FieldValue.serverTimestamp());
            db.collection(COLLECTION_NOTIFICATIONS).add(notification)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save SOS notification", e));
        }
        return notified.size();
    }

    static String titleFor(boolean isPickup, String statusKey) {
        if (STATUS_DONE.equals(statusKey)) {
            return isPickup ? "Picked up" : "Dropped off";
        }
        return isPickup ? "Not picked up" : "Not dropped off";
    }

    static String messageFor(boolean isPickup, String statusKey, String childName) {
        switch (statusKey) {
            case STATUS_DONE:
                return isPickup
                        ? childName + " has been picked up by the school bus."
                        : childName + " has been dropped off by the school bus.";
            case STATUS_NOT_DONE:
                return isPickup
                        ? childName + " was marked as not picked up today. The school has been told."
                        : childName + " was marked as not dropped off. The school has been told and will contact you.";
            default:
                return isPickup
                        ? "The driver did not confirm that " + childName + " was picked up. The school has been told."
                        : "The driver did not confirm that " + childName + " was dropped off. The school has been told and will contact you.";
        }
    }
}
