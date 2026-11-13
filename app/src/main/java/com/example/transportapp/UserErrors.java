package com.example.transportapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Friendly error messages (review #5). The exception text can contain internal paths, IDs or
 * personal data, so it only goes to the log; the user sees what failed and what to do next.
 */
final class UserErrors {

    private static final String TAG = "UserErrors";

    private UserErrors() {}

    /** @param whatFailed short sentence such as "Could not load routes" */
    static void show(Context context, String whatFailed, Exception e) {
        Log.w(TAG, whatFailed, e);
        Toast.makeText(context, whatFailed + ". " + context.getString(hintFor(e)),
                Toast.LENGTH_LONG).show();
    }

    private static int hintFor(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return R.string.error_hint_permission;
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return R.string.error_hint_connection;
            }
        }
        if (e instanceof FirebaseNetworkException) return R.string.error_hint_connection;
        return R.string.error_hint_retry;
    }
}
