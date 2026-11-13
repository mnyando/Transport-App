package com.example.transportapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Edge-to-edge for every screen (review section 5).
 *
 * The app draws behind the status bar and the gesture/navigation bar (Android 15 forces this),
 * so each screen's root view is padded by the real size of those bars and of the keyboard,
 * instead of the old fixed 24 dp toolbar margin, which was wrong on phones with a notch or a
 * taller status bar. Screens with the green toolbar get a green strip behind the status bar
 * (with light icons) so the bar looks continuous; screens with the green footer get the same
 * at the bottom.
 */
final class SystemBars {

    private SystemBars() {}

    static void applyInsets(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);

        boolean hasToolbar = activity.findViewById(R.id.toolbar) != null;
        boolean hasFooter = activity.findViewById(R.id.footer_text) != null;
        int barColor = ContextCompat.getColor(activity, R.color.brand_primary);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), root);
        if (hasToolbar) controller.setAppearanceLightStatusBars(false);
        if (hasFooter) controller.setAppearanceLightNavigationBars(false);

        int padLeft = root.getPaddingLeft();
        int padTop = root.getPaddingTop();
        int padRight = root.getPaddingRight();
        int padBottom = root.getPaddingBottom();
        Drawable originalBackground = root.getBackground();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, ime.bottom);

            v.setPadding(padLeft + bars.left, padTop + bars.top,
                    padRight + bars.right, padBottom + bottom);

            if (hasToolbar || hasFooter) {
                v.setBackground(withBarStrips(originalBackground, barColor,
                        hasToolbar ? bars.top : 0,
                        hasFooter && ime.bottom == 0 ? bars.bottom : 0));
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** The screen's own background with brand-coloured strips behind the system bars. */
    private static Drawable withBarStrips(Drawable base, int color, int top, int bottom) {
        Drawable bottomLayer = base != null ? base : new ColorDrawable(Color.TRANSPARENT);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{
                bottomLayer, new ColorDrawable(color), new ColorDrawable(color)});
        layers.setLayerGravity(1, Gravity.TOP | Gravity.FILL_HORIZONTAL);
        layers.setLayerHeight(1, top);
        layers.setLayerGravity(2, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
        layers.setLayerHeight(2, bottom);
        return layers;
    }
}
