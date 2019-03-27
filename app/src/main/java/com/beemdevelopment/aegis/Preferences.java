package com.beemdevelopment.aegis;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private SharedPreferences _prefs;

    public Preferences(Context context) {
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isDarkModeEnabled() {
        return _prefs.getBoolean("pref_dark_mode", false);
    }

    public boolean isTapToRevealEnabled() {
        return _prefs.getBoolean("pref_tap_to_reveal", false);
    }

    public boolean isSecureScreenEnabled() {
        return _prefs.getBoolean("pref_secure_screen", true);
    }

    public boolean isAccountNameVisible() {
        return _prefs.getBoolean("pref_account_name", false);
    }

    public boolean isIntroDone() {
        return _prefs.getBoolean("pref_intro", false);
    }

    public void setIntroDone(boolean done) {
        _prefs.edit().putBoolean("pref_intro", done).apply();
    }

    public void setTapToRevealTime(int number) {
        _prefs.edit().putInt("pref_tap_to_reveal_time", number).apply();
    }

    public int getTapToRevealTime() {
        return _prefs.getInt("pref_tap_to_reveal_time", 30);
    }

    public int getCurrentTheme() {
        return _prefs.getInt("pref_current_theme", 0);
    }

    public void setCurrentTheme(Theme theme) {
        _prefs.edit().putInt("pref_current_theme", theme.ordinal()).apply();

    }


    public int getTimeout() {
        return _prefs.getInt("pref_timeout", -1);
    }
}
