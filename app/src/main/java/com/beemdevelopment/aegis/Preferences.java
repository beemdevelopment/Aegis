package com.beemdevelopment.aegis;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Locale;

public class Preferences {
    private SharedPreferences _prefs;

    public Preferences(Context context) {
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isTapToRevealEnabled() {
        return _prefs.getBoolean("pref_tap_to_reveal", false);
    }

    public boolean isSecureScreenEnabled() {
        // screen security should be enabled by default, but not for debug builds
        return _prefs.getBoolean("pref_secure_screen", !BuildConfig.DEBUG);
    }

    public boolean isAccountNameVisible() {
        return _prefs.getBoolean("pref_account_name", false);
    }

    public boolean isIntroDone() {
        return _prefs.getBoolean("pref_intro", false);
    }

    public boolean isAutoLockEnabled() {
        return _prefs.getBoolean("pref_auto_lock", true);
    }

    public void setIntroDone(boolean done) {
        _prefs.edit().putBoolean("pref_intro", done).apply();
    }

    public void setTapToRevealTime(int number) {
        _prefs.edit().putInt("pref_tap_to_reveal_time", number).apply();
    }

    public void setCurrentSortCategory(SortCategory category) {
        _prefs.edit().putInt("pref_current_sort_category", category.ordinal()).apply();
    }

    public SortCategory getCurrentSortCategory() {
        return SortCategory.fromInteger(_prefs.getInt("pref_current_sort_category", 0));
    }

    public int getTapToRevealTime() {
        return _prefs.getInt("pref_tap_to_reveal_time", 30);
    }

    public Theme getCurrentTheme() {
        return Theme.fromInteger(_prefs.getInt("pref_current_theme", 0));
    }

    public void setCurrentTheme(Theme theme) {
        _prefs.edit().putInt("pref_current_theme", theme.ordinal()).apply();
    }

    public ViewMode getCurrentViewMode() {
        return ViewMode.fromInteger(_prefs.getInt("pref_current_view_mode", 0));
    }

    public void setCurrentViewMode(ViewMode viewMode) {
        _prefs.edit().putInt("pref_current_view_mode", viewMode.ordinal()).apply();
    }

    public int getTimeout() {
        return _prefs.getInt("pref_timeout", -1);
    }

    public Locale getLocale() {
        String lang = _prefs.getString("pref_lang", "system");

        if (lang.equals("system")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Resources.getSystem().getConfiguration().getLocales().get(0);
            } else {
                return Resources.getSystem().getConfiguration().locale;
            }
        }

        return new Locale(lang);
    }
}
