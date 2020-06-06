package com.beemdevelopment.aegis;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Preferences {
    private SharedPreferences _prefs;

    public Preferences(Context context) {
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (getPasswordReminderTimestamp().getTime() == 0) {
            resetPasswordReminderTimestamp();
        }
    }

    public boolean isTapToRevealEnabled() {
        return _prefs.getBoolean("pref_tap_to_reveal", false);
    }

    public boolean isSearchAccountNameEnabled() {
        return _prefs.getBoolean("pref_search_names", false);
    }

    public boolean isEntryHighlightEnabled() {
        return _prefs.getBoolean("pref_highlight_entry", false);
    }

    public boolean isSecureScreenEnabled() {
        // screen security should be enabled by default, but not for debug builds
        return _prefs.getBoolean("pref_secure_screen", !BuildConfig.DEBUG);
    }

    public boolean isPasswordReminderEnabled() {
        return _prefs.getBoolean("pref_password_reminder", true);
    }

    public boolean isPasswordReminderNeeded() {
        long diff = new Date().getTime() - getPasswordReminderTimestamp().getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return isPasswordReminderEnabled() && days >= 30;
    }

    public Date getPasswordReminderTimestamp() {
        return new Date(_prefs.getLong("pref_password_reminder_counter", 0));
    }

    public void resetPasswordReminderTimestamp() {
        _prefs.edit().putLong("pref_password_reminder_counter", new Date().getTime()).apply();
    }

    public boolean isAccountNameVisible() {
        return _prefs.getBoolean("pref_account_name", true);
    }

    public int getCodeGroupSize() {
        if (_prefs.getBoolean("pref_code_group_size", false)) {
            return 2;
        } else {
            return 3;
        }
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
        return Theme.fromInteger(_prefs.getInt("pref_current_theme", Theme.SYSTEM.ordinal()));
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

    public boolean isBackupsEnabled() {
        return _prefs.getBoolean("pref_backups", false);
    }

    public void setIsBackupsEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_backups", enabled).apply();
    }

    public Uri getBackupsLocation() {
        String str = _prefs.getString("pref_backups_location", null);
        if (str != null) {
            return Uri.parse(str);
        }

        return null;
    }

    public void setBackupsLocation(Uri location) {
        _prefs.edit().putString("pref_backups_location", location == null ? null : location.toString()).apply();
    }

    public int getBackupsVersionCount() {
        return _prefs.getInt("pref_backups_versions", 5);
    }

    public void setBackupsVersionCount(int versions) {
        _prefs.edit().putInt("pref_backups_versions", versions).apply();
    }

    public boolean isTimeSyncWarningEnabled() {
        return _prefs.getBoolean("pref_warn_time_sync", true);
    }

    public void setIsTimeSyncWarningEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_warn_time_sync", enabled).apply();
    }

    public boolean isCopyOnTapEnabled() {
        return _prefs.getBoolean("pref_copy_on_tap", false);
    }
}
