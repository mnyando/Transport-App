package com.example.transportapp;

import android.util.Patterns;

import java.util.Locale;

/**
 * Parents are linked to students by the email address of their login, never by a typed name.
 * Emails are stored lower-cased so the student record, the notification and the signed-in
 * user's token email can be compared exactly (the Firestore rules do the same comparison).
 */
public final class ParentEmails {

    private ParentEmails() {}

    /** Trims and lower-cases an email; returns "" for null. */
    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
