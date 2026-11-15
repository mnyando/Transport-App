package com.example.transportapp;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records in this app point at each other by <b>name</b>: students store their route and bus
 * names, staff store their assigned bus and route names, and routes store their bus name. These
 * helpers let the record screens check those links before a rename or delete would break them,
 * and carry a route change through to the students on it.
 */
final class RecordLinks {

    /** Firestore allows at most 500 writes per batch. */
    private static final int BATCH_LIMIT = 450;

    private RecordLinks() {}

    /** Children whose record names this route. */
    static Task<Long> studentsOnRoute(FirebaseFirestore db, String routeName) {
        return count(db.collectionGroup(FirestorePaths.STUDENT_LIST).whereEqualTo("route", routeName));
    }

    /** Children whose record names this bus. */
    static Task<Long> studentsOnVehicle(FirebaseFirestore db, String vehicleName) {
        return count(db.collectionGroup(FirestorePaths.STUDENT_LIST).whereEqualTo("vehicle", vehicleName));
    }

    static Task<Long> staffWith(FirebaseFirestore db, String field, String value) {
        return count(db.collection(FirestorePaths.STAFF).whereEqualTo(field, value));
    }

    static Task<Long> routesForVehicle(FirebaseFirestore db, String vehicleName) {
        return count(db.collection(FirestorePaths.ROUTES).whereEqualTo("vehicle", vehicleName));
    }

    /**
     * True when another document in {@code collection} already has {@code field == value}.
     * @param excludeId the record being edited, which may keep its own name.
     */
    static Task<Boolean> nameTaken(FirebaseFirestore db, String collection, String field,
                                   String value, @Nullable String excludeId) {
        return db.collection(collection).whereEqualTo(field, value).limit(2).get()
                .continueWith(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    for (DocumentSnapshot doc : t.getResult()) {
                        if (!doc.getId().equals(excludeId)) return true;
                    }
                    return false;
                });
    }

    /**
     * Copies a changed route onto every student and staff record that names it.
     * Students keep their own copy of the route name, bus and times, so without this a renamed
     * route or a new bus would leave them on the old one.
     *
     * @return the parent emails of the moved students, so the caller can add them to the new
     *         bus group.
     */
    static Task<List<String>> applyRouteChange(FirebaseFirestore db, String oldName, String newName,
                                               String vehicle, String pickupTime, String dropOffTime) {
        Task<QuerySnapshot> students = db.collectionGroup(FirestorePaths.STUDENT_LIST)
                .whereEqualTo("route", oldName).get();
        Task<QuerySnapshot> staff = oldName.equals(newName) ? Tasks.forResult(null)
                : db.collection(FirestorePaths.STAFF).whereEqualTo("assignedRoute", oldName).get();

        return Tasks.whenAllComplete(students, staff).continueWithTask(t -> {
            if (!students.isSuccessful()) throw students.getException();
            if (!staff.isSuccessful()) throw staff.getException();

            List<String> parentEmails = new ArrayList<>();
            List<Task<Void>> commits = new ArrayList<>();
            WriteBatch batch = db.batch();
            int writes = 0;

            Map<String, Object> studentFields = new HashMap<>();
            studentFields.put("route", newName);
            studentFields.put("vehicle", vehicle);
            studentFields.put("pickupTime", pickupTime);
            studentFields.put("dropOffTime", dropOffTime);

            for (DocumentSnapshot doc : students.getResult()) {
                batch.update(doc.getReference(), studentFields);
                String email = doc.getString("parentEmail");
                if (email != null && !email.isEmpty()) parentEmails.add(email);
                if (++writes == BATCH_LIMIT) {
                    commits.add(batch.commit());
                    batch = db.batch();
                    writes = 0;
                }
            }
            if (staff.getResult() != null) {
                for (DocumentSnapshot doc : staff.getResult()) {
                    batch.update(doc.getReference(), "assignedRoute", newName);
                    if (++writes == BATCH_LIMIT) {
                        commits.add(batch.commit());
                        batch = db.batch();
                        writes = 0;
                    }
                }
            }
            if (writes > 0) commits.add(batch.commit());
            return Tasks.whenAll(commits).continueWith(done -> {
                if (!done.isSuccessful()) throw done.getException();
                return parentEmails;
            });
        });
    }

    private static Task<Long> count(Query query) {
        return query.count().get(AggregateSource.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            return t.getResult().getCount();
        });
    }
}
