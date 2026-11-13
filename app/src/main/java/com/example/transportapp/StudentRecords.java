package com.example.transportapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Saves a student for Add Student and Modify Student (review #13).
 *
 * <ul>
 *   <li>The bus capacity check and the save happen in one Firestore transaction against the
 *       vehicle's {@code studentCount}, so two admins cannot overfill a bus. (The old check
 *       counted children on the <i>route</i>, in a separate step, and counted the child being
 *       edited, so a full bus blocked every edit.)</li>
 *   <li>A new student whose ID number is already used in that grade is refused instead of
 *       silently overwriting the other child.</li>
 *   <li>Editing keeps the trip fields (statuses, current trip) instead of replacing the whole
 *       record; moving grade moves the full record.</li>
 *   <li>Driver and attendant names are no longer copied onto each child, where they went stale.
 *       The {@code syncBusStaff} Cloud Function keeps them on {@code busGroups/{bus}} instead,
 *       and the parent dashboard reads them from there.</li>
 * </ul>
 * The {@code recountVehicleStudents} Cloud Function recounts {@code studentCount} after every
 * change, so the count also stays right after deletes, console edits and older data.
 */
final class StudentRecords {

    private static final String TAG = "StudentRecords";

    /** Copied staff fields from older records, removed on the next save. */
    private static final String[] LEGACY_STAFF_FIELDS =
            {"driverName", "driverPhone", "attendantName", "attendantPhone"};

    private StudentRecords() {}

    /** Where an existing student lives now; null when adding. */
    static final class Existing {
        final String grade;
        final String docId;

        Existing(String grade, String docId) {
            this.grade = grade;
            this.docId = docId;
        }
    }

    interface Result {
        void onSaved(String vehicle);

        /** {@code message} is already user-friendly when non-null. */
        void onFailed(@Nullable String message, @Nullable Exception e);
    }

    /** A reason the save was refused, with a message for the school. */
    static final class Refused extends FirebaseFirestoreException {
        @StringRes final int messageRes;
        final Object[] args;

        Refused(@StringRes int messageRes, Object... args) {
            super("refused", Code.ABORTED);
            this.messageRes = messageRes;
            this.args = args;
        }
    }

    /**
     * @param fields name, studentId, parentName, parentContact, parentEmail, homeLocation
     */
    static void save(Context context, FirebaseFirestore db, Map<String, Object> fields, String grade,
                     String route, @Nullable Existing existing, Result result) {
        String docId = existing != null ? existing.docId : String.valueOf(fields.get("studentId"));
        DocumentReference target = studentRef(db, grade, docId);
        DocumentReference source = existing != null ? studentRef(db, existing.grade, existing.docId) : null;

        Task<QuerySnapshot> routeTask = db.collection(FirestorePaths.ROUTES)
                .whereEqualTo("routeName", route).limit(1).get();
        Task<DocumentSnapshot> sourceTask = source != null ? source.get() : Tasks.forResult(null);

        Tasks.whenAllComplete(routeTask, sourceTask).continueWithTask(t -> {
            if (!routeTask.isSuccessful()) throw routeTask.getException();
            if (!sourceTask.isSuccessful()) throw sourceTask.getException();
            if (routeTask.getResult().isEmpty()) throw new Refused(R.string.error_route_not_found, route);
            DocumentSnapshot routeDoc = routeTask.getResult().getDocuments().get(0);
            String vehicle = routeDoc.getString("vehicle");
            if (vehicle == null || vehicle.isEmpty()) throw new Refused(R.string.error_route_no_vehicle, route);

            DocumentSnapshot current = sourceTask.getResult();
            if (existing != null && (current == null || !current.exists())) {
                throw new Refused(R.string.student_already_removed);
            }
            String oldVehicle = current != null ? current.getString("vehicle") : null;
            boolean joiningBus = !vehicle.equals(oldVehicle);

            Map<String, Object> data = new HashMap<>(fields);
            data.put("grade", grade);
            data.put("route", route);
            data.put("vehicle", vehicle);
            data.put("pickupTime", routeDoc.getString("pickupTime"));
            data.put("dropOffTime", routeDoc.getString("dropOffTime"));

            return findVehicle(db, vehicle).continueWithTask(vt -> {
                DocumentReference vehicleRef = vt.getResult();
                if (vehicleRef == null) throw new Refused(R.string.error_vehicle_not_found, vehicle);
                Task<DocumentReference> oldVehicleTask = joiningBus && oldVehicle != null
                        ? findVehicle(db, oldVehicle) : Tasks.forResult(null);
                Task<Long> baselineTask = joiningBus ? countOnBus(db, vehicle) : Tasks.forResult(0L);
                return Tasks.whenAllComplete(oldVehicleTask, baselineTask).continueWithTask(x -> {
                    DocumentReference oldVehicleRef = oldVehicleTask.isSuccessful() ? oldVehicleTask.getResult() : null;
                    long baseline = baselineTask.isSuccessful() && baselineTask.getResult() != null
                            ? baselineTask.getResult() : 0L;
                    return runTransaction(db, data, target, source, current, vehicleRef, oldVehicleRef,
                            joiningBus, baseline).continueWith(done -> vehicle);
                });
            });
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String vehicle = task.getResult();
                // Lets this parent see the bus location and join the bus chat.
                Object email = fields.get("parentEmail");
                if (email != null) BusGroups.addParent(db, vehicle, email.toString());
                result.onSaved(vehicle);
                return;
            }
            Exception e = task.getException();
            Refused refused = findRefused(e);
            if (refused != null) {
                result.onFailed(context.getString(refused.messageRes, refused.args), null);
            } else {
                Log.w(TAG, "Student not saved", e);
                result.onFailed(null, e);
            }
        });
    }

    private static Task<Void> runTransaction(FirebaseFirestore db, Map<String, Object> data,
                                             DocumentReference target, @Nullable DocumentReference source,
                                             @Nullable DocumentSnapshot current, DocumentReference vehicleRef,
                                             @Nullable DocumentReference oldVehicleRef, boolean joiningBus,
                                             long baseline) {
        boolean moving = source != null && !source.getPath().equals(target.getPath());
        return db.runTransaction(tx -> {
            // All reads first (a Firestore rule for transactions).
            DocumentSnapshot bus = tx.get(vehicleRef);
            DocumentSnapshot oldBus = joiningBus && oldVehicleRef != null ? tx.get(oldVehicleRef) : null;
            DocumentSnapshot clash = source == null || moving ? tx.get(target) : null;

            if (clash != null && clash.exists()) {
                throw new Refused(R.string.error_student_id_taken, data.get("studentId"), data.get("grade"));
            }
            long count = 0;
            if (joiningBus) {
                Long stored = bus.getLong("studentCount");
                count = stored != null ? stored : baseline;
                int capacity = Vehicle.parseCapacity(bus.get("capacity"));
                if (capacity <= 0) throw new Refused(R.string.error_vehicle_capacity_missing, data.get("vehicle"));
                if (count >= capacity) throw new Refused(R.string.error_vehicle_full, data.get("vehicle"), capacity);
            }

            if (source == null) {
                tx.set(target, data);
            } else if (moving) {
                Map<String, Object> full = new HashMap<>();
                if (current != null && current.getData() != null) full.putAll(current.getData());
                for (String f : LEGACY_STAFF_FIELDS) full.remove(f);
                full.putAll(data);
                tx.set(target, full);
                tx.delete(source);
            } else {
                Map<String, Object> merge = new HashMap<>(data);
                for (String f : LEGACY_STAFF_FIELDS) merge.put(f, FieldValue.delete());
                tx.set(target, merge, SetOptions.merge());
            }

            if (joiningBus) {
                tx.update(vehicleRef, "studentCount", count + 1);
                if (oldBus != null && oldBus.exists()) {
                    Long old = oldBus.getLong("studentCount");
                    if (old != null) tx.update(oldVehicleRef, "studentCount", Math.max(0, old - 1));
                }
            }
            return null;
        });
    }

    private static DocumentReference studentRef(FirebaseFirestore db, String grade, String docId) {
        return db.collection(FirestorePaths.STUDENTS).document(grade)
                .collection(FirestorePaths.STUDENT_LIST).document(docId);
    }

    /** Vehicles are looked up by name (students store the bus name). */
    private static Task<DocumentReference> findVehicle(FirebaseFirestore db, String name) {
        return db.collection(FirestorePaths.VEHICLES).whereEqualTo("vehicleName", name).limit(1).get()
                .continueWith(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    return t.getResult().isEmpty() ? null : t.getResult().getDocuments().get(0).getReference();
                });
    }

    /** Used only when a vehicle has no studentCount yet (records from before review #13). */
    private static Task<Long> countOnBus(FirebaseFirestore db, String vehicle) {
        return db.collectionGroup(FirestorePaths.STUDENT_LIST).whereEqualTo("vehicle", vehicle)
                .count().get(AggregateSource.SERVER)
                .continueWith(t -> t.isSuccessful() ? t.getResult().getCount() : 0L);
    }

    @Nullable
    private static Refused findRefused(@Nullable Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof Refused) return (Refused) t;
            if (t.getCause() == t) break;
        }
        return null;
    }

    /** Shows the outcome of a failed save. */
    static void showFailure(Context context, @Nullable String message, @Nullable Exception e) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } else {
            UserErrors.show(context, context.getString(R.string.error_student_not_saved), e);
        }
    }
}
