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
 * Edge-to-edge for every screen.
 *
 * The app draws behind the status bar and the gesture/navigation bar (Android 15 forces this),
 * so each screen's root view is padded by the real size of those bars and of the keyboard.
 *
 * Screens with the shared gradient toolbar get the same gradient painted behind the status bar
 * (with light icons) so the bar looks continuous. Screens whose root view has
 * {@code android:tag="hero"} draw their own gradient hero under the status bar: they get light
 * status icons and no strip, and are not padded at the top (the hero pads itself via
 * {@link #HERO_TOP_TAG} children, see {@link #applyInsets}).
 */
final class SystemBars {

    /** Root tag for screens that draw a gradient hero edge to edge under the status bar. */
    static final String HERO_TAG = "hero";
    /** Tag for the view inside a hero screen that should be pushed below the status bar. */
    static final String HERO_TOP_TAG = "hero_top";

    private SystemBars() {}

    static void applyInsets(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);

        boolean hasToolbar = activity.findViewById(R.id.toolbar) != null;
        boolean isHero = HERO_TAG.equals(root.getTag());
        View heroTop = isHero ? root.findViewWithTag(HERO_TOP_TAG) : null;

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), root);
        if (hasToolbar || isHero) controller.setAppearanceLightStatusBars(false);

        int padLeft = root.getPaddingLeft();
        int padTop = root.getPaddingTop();
        int padRight = root.getPaddingRight();
        int padBottom = root.getPaddingBottom();
        int heroTopPad = heroTop != null ? heroTop.getPaddingTop() : 0;
        Drawable originalBackground = root.getBackground();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, ime.bottom);

            if (isHero) {
                // The hero runs under the status bar; only its content is pushed down.
                v.setPadding(padLeft + bars.left, padTop, padRight + bars.right, padBottom + bottom);
                if (heroTop != null) {
                    heroTop.setPadding(heroTop.getPaddingLeft(), heroTopPad + bars.top,
                            heroTop.getPaddingRight(), heroTop.getPaddingBottom());
                }
            } else {
                v.setPadding(padLeft + bars.left, padTop + bars.top,
                        padRight + bars.right, padBottom + bottom);
                if (hasToolbar) {
                    v.setBackground(withStatusStrip(activity, originalBackground, bars.top));
                }
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** The screen's own background with the toolbar gradient behind the status bar. */
    private static Drawable withStatusStrip(Activity activity, Drawable base, int top) {
        Drawable bottomLayer = base != null ? base : new ColorDrawable(Color.TRANSPARENT);
        Drawable strip = ContextCompat.getDrawable(activity, R.drawable.bg_header_gradient);
        if (strip == null) {
            strip = new ColorDrawable(ContextCompat.getColor(activity, R.color.brand_primary));
        }
        LayerDrawable layers = new LayerDrawable(new Drawable[]{bottomLayer, strip.mutate()});
        layers.setLayerGravity(1, Gravity.TOP | Gravity.FILL_HORIZONTAL);
        layers.setLayerHeight(1, top);
        return layers;
    }
}
