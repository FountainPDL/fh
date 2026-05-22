package com.fountainhome.streaming.service;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ThemeHelper {

    /**
     * Apply background and accent colors to entire activity window
     */
    public static void apply(Activity activity) {
        int bg      = AppPreferences.getBackgroundColor(activity);
        int surface = AppPreferences.getSurfaceColor(activity);
        int accent  = AppPreferences.getAccentColor(activity);
        int text    = AppPreferences.getTextColor(activity);

        // Window background
        if (activity.getWindow() != null) {
            activity.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(bg));
            activity.getWindow().setStatusBarColor(surface);
            activity.getWindow().setNavigationBarColor(surface);
        }

        // Apply to root view
        View root = activity.getWindow().getDecorView().getRootView();
        applyToView(root, bg, surface, accent, text);
    }

    /**
     * Apply accent color to specific views (buttons, tabs, etc.)
     */
    private static void applyToView(View view, int bg, int surface, int accent, int text) {
        if (view == null) return;

        // Bottom nav accent
        if (view instanceof BottomNavigationView) {
            BottomNavigationView bnv = (BottomNavigationView) view;
            bnv.setBackgroundColor(surface);
            // Active item uses accent color
            android.content.res.ColorStateList csl = new android.content.res.ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
                },
                new int[]{ accent, 0xFF666666 }
            );
            bnv.setItemIconTintList(csl);
            bnv.setItemTextColor(csl);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToView(group.getChildAt(i), bg, surface, accent, text);
            }
        }
    }

    /**
     * Tint a view with accent color
     */
    public static void accentView(View view, int accentColor) {
        if (view == null) return;
        view.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(accentColor));
    }
}
