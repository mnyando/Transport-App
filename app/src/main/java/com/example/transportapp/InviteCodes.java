package com.example.transportapp;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * The parent sign-up code (review #2). Sign-up is refused by firestore.rules unless the code the
 * parent types matches {@code config/signup.inviteCode}. Only school accounts can read or change
 * it; the rules compare the code server-side, so the app never needs to read it at sign-up.
 */
final class InviteCodes {

    static final String CONFIG = "config";
    static final String SIGNUP_DOC = "signup";
    /** No 0/O or 1/I, so codes are easy to read aloud and type. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private InviteCodes() {}

    static String randomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return code.toString();
    }

    static void showDialog(Activity activity) {
        DocumentReference doc = FirebaseFirestore.getInstance().collection(CONFIG).document(SIGNUP_DOC);
        doc.get()
                .addOnSuccessListener(snapshot -> {
                    if (activity.isFinishing()) return;
                    String current = snapshot.getString("inviteCode");
                    String message = activity.getString(R.string.invite_code_message) + "\n\n"
                            + (current == null || current.isEmpty()
                            ? activity.getString(R.string.invite_code_none)
                            : activity.getString(R.string.invite_code_current, current));
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.invite_code_title)
                            .setMessage(message)
                            .setPositiveButton(R.string.invite_code_generate, (d, w) -> save(activity, doc, randomCode()))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .addOnFailureListener(e -> UserErrors.show(activity, activity.getString(R.string.invite_code_load_failed), e));
    }

    private static void save(Activity activity, DocumentReference doc, String code) {
        Map<String, Object> data = new HashMap<>();
        data.put("inviteCode", code);
        data.put("updatedAt", FieldValue.serverTimestamp());
        doc.set(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(activity, R.string.invite_code_saved, Toast.LENGTH_SHORT).show();
                    showDialog(activity);
                })
                .addOnFailureListener(e -> UserErrors.show(activity, activity.getString(R.string.invite_code_load_failed), e));
    }
}
