package com.example.transportapp;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Keeps this phone's FCM registration token on the signed-in user's document
 * ({@code users/{uid}.fcmTokens}), so the {@code sendParentPush} Cloud Function knows where to
 * deliver alerts. Tokens are only ever written to the user's own document.
 */
public final class PushTokens {

    private static final String TAG = "PushTokens";
    static final String FIELD_TOKENS = "fcmTokens";

    private PushTokens() {}

    /** Fetches the current token and saves it for the signed-in user. Safe to call often. */
    public static void registerCurrentDevice() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(PushTokens::saveToken)
                .addOnFailureListener(e -> Log.w(TAG, "Could not get FCM token", e));
    }

    /** Saves a token for the signed-in user; does nothing when signed out. */
    public static void saveToken(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token == null || token.isEmpty()) return;
        FirebaseFirestore.getInstance().collection(FirestorePaths.USERS).document(user.getUid())
                .update(FIELD_TOKENS, FieldValue.arrayUnion(token))
                .addOnFailureListener(e -> Log.w(TAG, "Could not save FCM token", e));
    }

    /**
     * Removes this phone's token so the signed-out user stops getting alerts here (important on
     * shared phones). Call before {@code FirebaseAuth.signOut()}, then run {@code afterwards}.
     */
    public static void unregisterCurrentDevice(Runnable afterwards) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            afterwards.run();
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> FirebaseFirestore.getInstance()
                        .collection(FirestorePaths.USERS).document(user.getUid())
                        .update(FIELD_TOKENS, FieldValue.arrayRemove(token))
                        .addOnCompleteListener(t -> afterwards.run()))
                .addOnFailureListener(e -> afterwards.run());
    }
}
