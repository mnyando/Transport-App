package com.example.transportapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementation of {@link ParentRepository}. Every query is limited to the parent's
 * own email; firestore.rules enforces the same limit on the server.
 */
public class FirestoreParentRepository implements ParentRepository {

    private final FirebaseFirestore db;

    public FirestoreParentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Children linked to the signed-in parent's account ID (review #1). The email is only used by
     * the server to make the link, after checking it is verified (see {@link ParentLinks}).
     */
    @Override
    public void loadChildren(String parentEmail, Callback<List<Student>> callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }
        ParentLinks.claim().continueWithTask(t -> db.collectionGroup(FirestorePaths.STUDENT_LIST)
                        .whereArrayContains("parentUids", uid)
                        .get())
                .addOnSuccessListener(snapshots -> {
                    List<Student> children = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        // Grade comes from the path: students/{grade}/studentList/{studentId}
                        student.setGrade(document.getReference().getParent().getParent().getId());
                        children.add(student);
                    }
                    addBusStaff(children, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Driver and attendant names live on busGroups/{bus} (kept current by the syncBusStaff Cloud
     * Function) instead of being copied onto every child, where they went stale (review #13).
     */
    private void addBusStaff(List<Student> children, Callback<List<Student>> callback) {
        Map<String, Task<DocumentSnapshot>> buses = new HashMap<>();
        for (Student child : children) {
            String id = BusGroups.docId(child.getVehicle());
            if (!id.isEmpty() && !buses.containsKey(id)) {
                buses.put(id, db.collection(FirestorePaths.BUS_GROUPS).document(id).get());
            }
        }
        Tasks.whenAllComplete(buses.values()).addOnCompleteListener(done -> {
            for (Student child : children) {
                Task<DocumentSnapshot> bus = buses.get(BusGroups.docId(child.getVehicle()));
                if (bus == null || !bus.isSuccessful() || bus.getResult() == null) continue;
                String driver = bus.getResult().getString("driverName");
                String attendant = bus.getResult().getString("attendantName");
                if (driver != null) child.setDriverName(driver);
                if (attendant != null) child.setAttendantName(attendant);
            }
            callback.onResult(children);
        });
    }

    @Override
    public Subscription listenToNotifications(String parentEmail, Callback<List<Notification>> callback) {
        // Newest 50 only, sorted by the server (review #14: this used to download every alert ever
        // sent). Needs the parentEmail + timestamp index in firestore.indexes.json.
        ListenerRegistration registration = db.collection(TripAlerts.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("parentEmail", parentEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(ParentDashboardViewModel.MAX_NOTIFICATIONS)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }
                    if (snapshots == null) return;
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        notifications.add(notification);
                    }
                    callback.onResult(notifications);
                });
        return registration::remove;
    }

    @Override
    public void loadBusLocation(String vehicle, Callback<BusLocation> callback) {
        db.collection(BusGroups.COLLECTION).document(BusGroups.docId(vehicle)).get()
                .addOnSuccessListener(doc -> callback.onResult(new BusLocation(
                        doc.getDouble("lat"),
                        doc.getDouble("lng"),
                        Boolean.TRUE.equals(doc.getBoolean("active")),
                        doc.getLong("updatedAtMillis"))))
                .addOnFailureListener(callback::onError);
    }
}
