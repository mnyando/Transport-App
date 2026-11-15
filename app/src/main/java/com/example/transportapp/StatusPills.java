package com.example.transportapp;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Colours a {@code Widget.Transport.Pill} TextView for an Active / Not Active record. */
final class StatusPills {

    private StatusPills() {}

    static void apply(TextView pill, String status) {
        String text = status != null && !status.isEmpty() ? status : "Unknown";
        boolean active = "Active".equalsIgnoreCase(text);
        boolean inactive = "Not Active".equalsIgnoreCase(text) || "Inactive".equalsIgnoreCase(text);

        int container = active ? R.color.status_success_container
                : inactive ? R.color.status_warning_container : R.color.surface;
        int content = active ? R.color.status_on_success
                : inactive ? R.color.status_on_warning : R.color.text_secondary;

        pill.setText(text);
        pill.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(pill.getContext(), container)));
        pill.setTextColor(ContextCompat.getColor(pill.getContext(), content));
    }
}
