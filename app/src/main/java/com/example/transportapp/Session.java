package com.example.transportapp;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Sign-out for the three home screens (School, Parent, Driver).
 *
 * The app now reopens a saved session straight into the home screen, so each home screen needs a
 * way out. On a home screen the toolbar shows a sign-out button instead of the back arrow, since
 * the home screen is the first screen in the task.
 */
final class Session {

    private Session() {}

    /**
     * @param beforeSignOut optional clean-up that must happen while still signed in, such as
     *                      stopping a driver's live location sharing.
     */
    static void setUpHomeToolbar(Activity activity, @Nullable Runnable beforeSignOut) {
        View back = activity.findViewById(R.id.backButton);
        if (back != null) back.setVisibility(View.GONE);

        View signOut = activity.findViewById(R.id.signOutButton);
        if (signOut == null) return;
        signOut.setVisibility(View.VISIBLE);
        signOut.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(R.string.sign_out)
                .setMessage(R.string.sign_out_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.sign_out, (d, w) -> signOut(activity, beforeSignOut))
                .show());
    }

    static void signOut(Activity activity, @Nullable Runnable beforeSignOut) {
        if (beforeSignOut != null) beforeSignOut.run();
        // Remove this phone's push token first, so alerts for this account stop arriving here.
        PushTokens.unregisterCurrentDevice(() -> {
            FirebaseAuth.getInstance().signOut();
            activity.startActivity(new Intent(activity, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity.finish();
        });
    }
}
