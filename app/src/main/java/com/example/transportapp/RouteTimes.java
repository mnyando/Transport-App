package com.example.transportapp;

import androidx.annotation.Nullable;

import java.util.Locale;

/** Route times are stored as 24-hour "HH:mm" text. */
final class RouteTimes {

    private RouteTimes() {}

    /** Always Latin digits, so the stored value is the same on every phone language. */
    static String format(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    /** {hour, minute} from "HH:mm", or 07:00 when empty or unreadable (a sensible pickup start). */
    static int[] parse(@Nullable String text) {
        int minutes = toMinutes(text);
        return minutes < 0 ? new int[]{7, 0} : new int[]{minutes / 60, minutes % 60};
    }

    /** Minutes after midnight, or -1 when not a valid "HH:mm". */
    static int toMinutes(@Nullable String text) {
        if (text == null) return -1;
        String[] parts = text.trim().split(":");
        if (parts.length != 2) return -1;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** True when both times are valid and drop-off comes after pickup. */
    static boolean dropOffAfterPickup(String pickup, String dropOff) {
        int p = toMinutes(pickup);
        int d = toMinutes(dropOff);
        return p >= 0 && d >= 0 && d > p;
    }
}
