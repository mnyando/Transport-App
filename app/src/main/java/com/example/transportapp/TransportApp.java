package com.example.transportapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * Application entry point.
 *
 * Many screens have a back arrow in their toolbar that was never given a click handler. Rather
 * than repeat the same code in every activity, this wires any back arrow that has no handler of
 * its own (an ImageButton/ImageView whose id is back_button/backButton or whose content
 * description is "Back") to the normal system back action. Screens that already handle their
 * back arrow are left alone.
 *
 * It also fills in the shared layouts (layout/toolbar.xml and layout/footer.xml): the toolbar
 * title is the activity's android:label from the manifest, and the footer shows the current year.
 */
public class TransportApp extends Application {

    private static final int TAG_WIRED = R.id.back_button_wired;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                View root = activity.getWindow().getDecorView();
                if (root.getTag(TAG_WIRED) != null) return;
                root.setTag(TAG_WIRED, Boolean.TRUE);
                wireBackButtons(activity, root);
                fillSharedViews(activity);
            }

            @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivityStopped(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });
    }

    /** Title from the activity's android:label; copyright year from the clock. */
    private void fillSharedViews(Activity activity) {
        TextView title = activity.findViewById(R.id.toolbar_title);
        if (title != null && TextUtils.isEmpty(title.getText())) {
            title.setText(activity.getTitle());
        }
        TextView footer = activity.findViewById(R.id.footer_text);
        if (footer != null) {
            footer.setText(getString(R.string.footer_copyright,
                    Calendar.getInstance().get(Calendar.YEAR)));
        }
    }

    private void wireBackButtons(Activity activity, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                wireBackButtons(activity, group.getChildAt(i));
            }
            return;
        }
        if (!(view instanceof ImageButton || view instanceof ImageView)) return;
        if (view.hasOnClickListeners() || !isBackButton(view)) return;

        view.setOnClickListener(v -> {
            if (activity instanceof ComponentActivity) {
                ((ComponentActivity) activity).getOnBackPressedDispatcher().onBackPressed();
            } else {
                activity.finish();
            }
        });
    }

    private boolean isBackButton(View view) {
        int id = view.getId();
        if (id != View.NO_ID) {
            try {
                String name = getResources().getResourceEntryName(id);
                if ("back_button".equals(name) || "backButton".equals(name)) return true;
            } catch (Exception ignored) {
                // id without a resource name
            }
        }
        CharSequence description = view.getContentDescription();
        if (description == null) return false;
        String d = description.toString().trim();
        return d.equalsIgnoreCase("Back") || d.equals(getString(R.string.back_button_desc));
    }
}
