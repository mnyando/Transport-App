package com.example.transportapp;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Map;

/**
 * Asks the {@code linkMyStaffAccount} Cloud Function to (re)link the signed-in, verified account
 * to its driver staff record by email. The normal link is made by triggers when the school saves
 * the staff record; this repairs links those triggers missed, so a driver no longer lands on the
 * parent dashboard until someone re-saves their record.
 */
final class StaffLinks {

    private static final String TAG = "StaffLinks";

    /** Why an account could not be linked (null when it was linked or the call failed). */
    static final String NO_STAFF = "no_staff";
    static final String NOT_DRIVER = "not_driver";
    static final String INACTIVE = "inactive";

    static final class Result {
        final boolean linked;
        @Nullable final String reason;

        Result(boolean linked, @Nullable String reason) {
            this.linked = linked;
            this.reason = reason;
        }
    }

    private StaffLinks() {}

    /** Always completes successfully; a failed call reads as "not linked, reason unknown". */
    static Task<Result> claim() {
        return FirebaseFunctions.getInstance().getHttpsCallable("linkMyStaffAccount").call()
                .continueWith(t -> {
                    if (!t.isSuccessful() || t.getResult() == null) {
                        Log.w(TAG, "Could not check driver link", t.getException());
                        return new Result(false, null);
                    }
                    Object data = t.getResult().getData();
                    if (!(data instanceof Map)) return new Result(false, null);
                    Map<?, ?> map = (Map<?, ?>) data;
                    boolean linked = Boolean.TRUE.equals(map.get("linked"));
                    Object reason = map.get("reason");
                    return new Result(linked, reason != null ? reason.toString() : null);
                });
    }
}
