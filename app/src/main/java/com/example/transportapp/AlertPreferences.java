package com.example.transportapp;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Which push alerts a parent wants (review section 5: "a notifications settings screen").
 *
 * Saved on the parent's own profile as {@code users/{uid}.alertPrefs = {pickup, dropoff}} and
 * applied by the {@code sendParentPush} Cloud Function, so it works even when the app is closed.
 * Only the routine "picked up" / "dropped off" confirmations can be turned off. Alerts that a
 * child was NOT picked up or dropped off, and SOS alerts, are always sent; every alert still
 * appears in the Notifications list on the parent page.
 */
final class AlertPreferences {

    static final String FIELD = "alertPrefs";
    static final String PICKUP = "pickup";
    static final String DROPOFF = "dropoff";

    private static final String TAG = "AlertPreferences";

    private AlertPreferences() {}

    static void showDialog(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DocumentReference profile = FirebaseFirestore.getInstance()
                .collection(FirestorePaths.USERS).document(user.getUid());

        profile.get()
                .addOnSuccessListener(doc -> {
                    if (activity.isFinishing()) return;
                    boolean[] checked = {
                            isOn(doc.get(FIELD + "." + PICKUP)),
                            isOn(doc.get(FIELD + "." + DROPOFF)),
                    };
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.alert_settings)
                            .setMultiChoiceItems(new CharSequence[]{
                                    activity.getString(R.string.alert_pref_pickup),
                                    activity.getString(R.string.alert_pref_dropoff),
                            }, checked, (d, which, isChecked) -> checked[which] = isChecked)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.save, (d, w) -> save(activity, profile, checked))
                            .setNeutralButton(R.string.alert_pref_always_on_title, (d, w) ->
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.alert_pref_always_on_title)
                                            .setMessage(R.string.alert_pref_always_on)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not load alert settings", e);
                    if (!activity.isFinishing()) {
                        Toast.makeText(activity, R.string.alert_settings_load_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Missing values mean "on": alerts are opt-out, never silently off. */
    private static boolean isOn(Object value) {
        return !(value instanceof Boolean) || (Boolean) value;
    }

    private static void save(Activity activity, DocumentReference profile, boolean[] checked) {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put(PICKUP, checked[0]);
        prefs.put(DROPOFF, checked[1]);
        profile.update(FIELD, prefs)
                .addOnSuccessListener(v -> Toast.makeText(activity,
                        R.string.alert_settings_saved, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not save alert settings", e);
                    Toast.makeText(activity, R.string.alert_settings_save_failed,
                            Toast.LENGTH_LONG).show();
                });
    }
}
