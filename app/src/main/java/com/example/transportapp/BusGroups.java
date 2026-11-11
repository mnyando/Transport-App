package com.example.transportapp;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One document per bus: {@code busGroups/{vehicle}}.
 *
 * <ul>
 *   <li>{@code parentEmails}: verified parent emails with a child on this bus. Firestore rules
 *       use it to decide who may see the bus location and join the bus chat.</li>
 *   <li>{@code lat}, {@code lng}, {@code accuracy}, {@code updatedAt}, {@code tripId},
 *       {@code active}: the live location written by {@link TripLocationService} while a trip
 *       is running.</li>
 *   <li>Sub-collection {@code messages}: the parent / driver / school chat for this bus.</li>
 * </ul>
 */
public final class BusGroups {

    private static final String TAG = "BusGroups";
    public static final String COLLECTION = "busGroups";
    public static final String MESSAGES = "messages";

    private BusGroups() {}

    /** Firestore document IDs cannot contain '/', so vehicle names are made safe. */
    public static String docId(String vehicle) {
        return vehicle == null ? "" : vehicle.trim().replace('/', '_');
    }

    /** School side: make sure a parent can see their child's bus. */
    public static void addParent(FirebaseFirestore db, String vehicle, String parentEmail) {
        String id = docId(vehicle);
        String email = ParentEmails.normalize(parentEmail);
        if (id.isEmpty() || email.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("vehicle", vehicle);
        data.put("parentEmails", FieldValue.arrayUnion(email));
        db.collection(COLLECTION).document(id).set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not update bus group", e));
    }

    /** Driver side: refresh the parent list from the students actually on this bus today. */
    public static void syncParents(FirebaseFirestore db, String vehicle, List<Student> students) {
        String id = docId(vehicle);
        if (id.isEmpty()) return;
        List<String> emails = new ArrayList<>();
        for (Student s : students) {
            String email = ParentEmails.normalize(s.getParentEmail());
            if (!email.isEmpty() && !emails.contains(email)) emails.add(email);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("vehicle", vehicle);
        data.put("parentEmails", emails);
        db.collection(COLLECTION).document(id).set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not sync bus group", e));
    }
}
