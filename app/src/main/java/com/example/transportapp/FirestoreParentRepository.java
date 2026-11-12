package com.example.transportapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore implementation of {@link ParentRepository}. Every query is limited to the parent's
 * own email; firestore.rules enforces the same limit on the server.
 */
public class FirestoreParentRepository implements ParentRepository {

    private final FirebaseFirestore db;

    public FirestoreParentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void loadChildren(String parentEmail, Callback<List<Student>> callback) {
        db.collectionGroup("studentList")
                .whereEqualTo("parentEmail", parentEmail)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Student> children = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        // Grade comes from the path: students/{grade}/studentList/{studentId}
                        student.setGrade(document.getReference().getParent().getParent().getId());
                        children.add(student);
                    }
                    callback.onResult(children);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public Subscription listenToNotifications(String parentEmail, Callback<List<Notification>> callback) {
        ListenerRegistration registration = db.collection(TripAlerts.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("parentEmail", parentEmail)
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
