package com.example.transportapp;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/**
 * The privacy notice users agree to when they create an account (review section 3: "get
 * recorded consent from parents, publish a privacy policy").
 *
 * <p>The text lives in {@code R.string.privacy_notice_text}. When it changes in a way users
 * must agree to again, raise {@link #VERSION}; the version each user agreed to is saved on
 * {@code users/{uid}.consentVersion}.
 */
public final class PrivacyNotice {

    /** Version of the notice text in strings.xml. */
    public static final int VERSION = 1;

    private PrivacyNotice() {}

    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.privacy_notice_title)
                .setMessage(R.string.privacy_notice_text)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
