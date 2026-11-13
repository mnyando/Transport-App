package com.example.transportapp;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;

/**
 * Links a verified parent's account to their children by account ID (review #1).
 *
 * The school records the parent's email on the student. The {@code linkMyChildren} Cloud
 * Function checks that the caller's email is verified and then adds their UID to
 * {@code parentUids} on each matching child (and removes it where the email no longer matches).
 * Children are then found, and protected by firestore.rules, by UID only.
 */
final class ParentLinks {

    private static final String TAG = "ParentLinks";

    private ParentLinks() {}

    /** Always completes successfully: if the function is unreachable, existing links still work. */
    static Task<Void> claim() {
        return FirebaseFunctions.getInstance().getHttpsCallable("linkMyChildren").call()
                .continueWith(t -> {
                    if (!t.isSuccessful()) Log.w(TAG, "Could not refresh parent links", t.getException());
                    return null;
                });
    }
}
