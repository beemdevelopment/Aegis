package com.beemdevelopment.aegis;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import com.beemdevelopment.aegis.util.JsonUtils;
import com.beemdevelopment.aegis.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Preferences {

    public static final int AUTO_LOCK_OFF = 1 << 0;

    public static final int AUTO_LOCK_ON_BACK_BUTTON = 1 << 1;

    public static final int AUTO_LOCK_ON_MINIMIZE = 1 << 2;

    public static final int AUTO_LOCK_ON_DEVICE_LOCK = 1 << 3;

    public static final int[] AUTO_LOCK_SETTINGS = { AUTO_LOCK_ON_BACK_BUTTON, AUTO_LOCK_ON_MINIMIZE, AUTO_LOCK_ON_DEVICE_LOCK };

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

    public boolean isEntryHighlightEnabled() {
        return _prefs.getBoolean("pref_highlight_entry", false);
    }

    public boolean isPauseFocusedEnabled() {
        boolean dependenciesEnabled = isTapToRevealEnabled() || isEntryHighlightEnabled();
        if (!dependenciesEnabled)
            return false;
        return _prefs.getBoolean("pref_pause_entry", false);
    }

    public boolean isPanicTriggerEnabled() {
        return _prefs.getBoolean("pref_panic_trigger", false);
    }

    public void setIsPanicTriggerEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_panic_trigger", enabled).apply();
    }

    public boolean isSecureScreenEnabled() {
        // screen security should be enabled by default, but not for debug builds
        return _prefs.getBoolean("pref_secure_screen", !BuildConfig.DEBUG);
    }

    public PassReminderFreq getPasswordReminderFrequency() {
        final String key = "pref_password_reminder_freq";
        if (_prefs.contains(key) || _prefs.getBoolean("pref_password_reminder", true)) {
            int i = _prefs.getInt(key, PassReminderFreq.BIWEEKLY.ordinal());
            return PassReminderFreq.fromInteger(i);
        }
        return PassReminderFreq.NEVER;
    }

    public void setPasswordReminderFrequency(PassReminderFreq freq) {
        _prefs.edit().putInt("pref_password_reminder_freq", freq.ordinal()).apply();
    }

    public boolean isPasswordReminderNeeded() {
        return isPasswordReminderNeeded(new Date().getTime());
    }

    boolean isPasswordReminderNeeded(long currTime) {
        PassReminderFreq freq = getPasswordReminderFrequency();
        if (freq == PassReminderFreq.NEVER) {
            return false;
        }
        long duration = currTime - getPasswordReminderTimestamp().getTime();
        return duration >= freq.getDurationMillis();
    }

    public Date getPasswordReminderTimestamp() {
        return new Date(_prefs.getLong("pref_password_reminder_counter", 0));
    }

    void setPasswordReminderTimestamp(long timestamp) {
        _prefs.edit().putLong("pref_password_reminder_counter", timestamp).apply();
    }

    public void resetPasswordReminderTimestamp() {
        setPasswordReminderTimestamp(new Date().getTime());
    }

    public boolean isAccountNameVisible() {
        return _prefs.getBoolean("pref_account_name", true);
    }

    public boolean isIconVisible() {
        return _prefs.getBoolean("pref_show_icons", true);
    }

    public CodeGrouping getCodeGroupSize() {
        String value = _prefs.getString("pref_code_group_size_string", "GROUPING_THREES");
        return CodeGrouping.valueOf(value);
    }

    public boolean isIntroDone() {
        return _prefs.getBoolean("pref_intro", false);
    }

    private int getAutoLockMask() {
        final int def = AUTO_LOCK_ON_BACK_BUTTON | AUTO_LOCK_ON_DEVICE_LOCK;
        if (!_prefs.contains("pref_auto_lock_mask")) {
            return _prefs.getBoolean("pref_auto_lock", true) ? def : AUTO_LOCK_OFF;
        }
        return _prefs.getInt("pref_auto_lock_mask", def);
    }

    public boolean isAutoLockEnabled() {
        return getAutoLockMask() != AUTO_LOCK_OFF;
    }

    public boolean isAutoLockTypeEnabled(int autoLockType) {
        return (getAutoLockMask() & autoLockType) == autoLockType;
    }

    public void setAutoLockMask(int autoLock) {
        _prefs.edit().putInt("pref_auto_lock_mask", autoLock).apply();
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

    public Integer getUsageCount(UUID uuid) {
        Integer usageCount = getUsageCounts().get(uuid);
        return usageCount != null ? usageCount : 0;
    }

    public void resetUsageCount(UUID uuid) {
        Map<UUID, Integer> usageCounts = getUsageCounts();
        usageCounts.put(uuid, 0);
        setUsageCount(usageCounts);
    }

    public void clearUsageCount() {
        _prefs.edit().remove("pref_usage_count").apply();
    }

    public Map<UUID, Integer> getUsageCounts() {
        Map<UUID, Integer> usageCounts = new HashMap<>();
        String usageCount = _prefs.getString("pref_usage_count", "");
        try {
            JSONArray arr = new JSONArray(usageCount);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject json = arr.getJSONObject(i);
                usageCounts.put(UUID.fromString(json.getString("uuid")), json.getInt("count"));
            }
        } catch (JSONException ignored) {
        }
        return usageCounts;
    }

    public void setUsageCount(Map<UUID, Integer> usageCounts) {
        JSONArray usageCountJson = new JSONArray();
        for (Map.Entry<UUID, Integer> entry : usageCounts.entrySet()) {
            JSONObject entryJson = new JSONObject();
            try {
                entryJson.put("uuid", entry.getKey());
                entryJson.put("count", entry.getValue());
                usageCountJson.put(entryJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        _prefs.edit().putString("pref_usage_count", usageCountJson.toString()).apply();
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
        String[] parts = lang.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        return new Locale(parts[0], parts[1]);
    }

    public boolean isAndroidBackupsEnabled() {
        return _prefs.getBoolean("pref_android_backups", false);
    }

    public void setIsAndroidBackupsEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_android_backups", enabled).apply();
        setAndroidBackupResult(null);
    }

    public boolean isBackupsEnabled() {
        return _prefs.getBoolean("pref_backups", false);
    }

    public void setIsBackupsEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_backups", enabled).apply();
        setBuiltInBackupResult(null);
    }

    public boolean isBackupReminderEnabled() {
        return _prefs.getBoolean("pref_backup_reminder", true);
    }

    public void setIsBackupReminderEnabled(boolean enabled) {
        _prefs.edit().putBoolean("pref_backup_reminder", enabled).apply();
    }

    public Uri getBackupsLocation() {
        String str = _prefs.getString("pref_backups_location", null);
        if (str != null) {
            return Uri.parse(str);
        }
        return null;
    }

    public boolean getFocusSearchEnabled() {
        return _prefs.getBoolean("pref_focus_search", false);
    }

    public void setFocusSearch(boolean enabled) {
        _prefs.edit().putBoolean("pref_focus_search", enabled).apply();
    }

    public void setLatestExportTimeNow() {
        _prefs.edit().putLong("pref_export_latest", new Date().getTime()).apply();
        setIsBackupReminderNeeded(false);
    }

    public Date getLatestBackupOrExportTime() {
        List<Date> dates = new ArrayList<>();
        long l = _prefs.getLong("pref_export_latest", 0);
        if (l > 0) {
            dates.add(new Date(l));
        }
        BackupResult builtinRes = getBuiltInBackupResult();
        if (builtinRes != null) {
            dates.add(builtinRes.getTime());
        }
        BackupResult androidRes = getAndroidBackupResult();
        if (androidRes != null) {
            dates.add(androidRes.getTime());
        }
        if (dates.size() == 0) {
            return null;
        }
        return Collections.max(dates, Date::compareTo);
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

    public void setAndroidBackupResult(@Nullable BackupResult res) {
        setBackupResult(false, res);
    }

    public void setBuiltInBackupResult(@Nullable BackupResult res) {
        setBackupResult(true, res);
    }

    @Nullable
    public BackupResult getAndroidBackupResult() {
        return getBackupResult(false);
    }

    @Nullable
    public BackupResult getBuiltInBackupResult() {
        return getBackupResult(true);
    }

    @Nullable
    public Preferences.BackupResult getErroredBackupResult() {
        Preferences.BackupResult res = getBuiltInBackupResult();
        if (res != null && !res.isSuccessful()) {
            return res;
        }
        res = getAndroidBackupResult();
        if (res != null && !res.isSuccessful()) {
            return res;
        }
        return null;
    }

    private void setBackupResult(boolean isBuiltInBackup, @Nullable BackupResult res) {
        String json = null;
        if (res != null) {
            res.setIsBuiltIn(isBuiltInBackup);
            json = res.toJson();
        }
        _prefs.edit().putString(getBackupResultKey(isBuiltInBackup), json).apply();
    }

    @Nullable
    private BackupResult getBackupResult(boolean isBuiltInBackup) {
        String json = _prefs.getString(getBackupResultKey(isBuiltInBackup), null);
        if (json == null) {
            return null;
        }
        try {
            BackupResult res = BackupResult.fromJson(json);
            res.setIsBuiltIn(isBuiltInBackup);
            return res;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String getBackupResultKey(boolean isBuiltInBackup) {
        return isBuiltInBackup ? "pref_backups_result_builtin" : "pref_backups_result_android";
    }

    public void setIsBackupReminderNeeded(boolean needed) {
        if (isBackupsReminderNeeded() != needed) {
            _prefs.edit().putBoolean("pref_backups_reminder_needed", needed).apply();
        }
    }

    public boolean isBackupsReminderNeeded() {
        return _prefs.getBoolean("pref_backups_reminder_needed", false);
    }

    public void setIsPlaintextBackupWarningNeeded(boolean needed) {
        _prefs.edit().putBoolean("pref_plaintext_backup_warning_needed", needed).apply();
    }

    public boolean isPlaintextBackupWarningNeeded() {
        return !isPlaintextBackupWarningDisabled() && _prefs.getBoolean("pref_plaintext_backup_warning_needed", false);
    }

    public void setIsPlaintextBackupWarningDisabled(boolean disabled) {
        _prefs.edit().putBoolean("pref_plaintext_backup_warning_disabled", disabled).apply();
    }

    public boolean isPlaintextBackupWarningDisabled() {
        return _prefs.getBoolean("pref_plaintext_backup_warning_disabled", false);
    }

    public boolean isPinKeyboardEnabled() {
        return _prefs.getBoolean("pref_pin_keyboard", false);
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

    public boolean isMinimizeOnCopyEnabled() {
        return _prefs.getBoolean("pref_minimize_on_copy", false);
    }

    public void setGroupFilter(List<String> groupFilter) {
        JSONArray json = new JSONArray(groupFilter);
        _prefs.edit().putString("pref_group_filter", json.toString()).apply();
    }

    public List<String> getGroupFilter() {
        String raw = _prefs.getString("pref_group_filter", null);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JSONArray json = new JSONArray(raw);
            List<String> filter = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                filter.add(json.isNull(i) ? null : json.optString(i));
            }
            return filter;
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    public static class BackupResult {

        private final Date _time;

        private boolean _isBuiltIn;

        private final String _error;

        public BackupResult(@Nullable Exception e) {
            this(new Date(), e == null ? null : e.toString());
        }

        private BackupResult(Date time, @Nullable String error) {
            _time = time;
            _error = error;
        }

        @Nullable
        public String getError() {
            return _error;
        }

        public boolean isSuccessful() {
            return _error == null;
        }

        public Date getTime() {
            return _time;
        }

        public String getElapsedSince(Context context) {
            return TimeUtils.getElapsedSince(context, _time);
        }

        public boolean isBuiltIn() {
            return _isBuiltIn;
        }

        private void setIsBuiltIn(boolean isBuiltIn) {
            _isBuiltIn = isBuiltIn;
        }

        public String toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("time", _time.getTime());
                obj.put("error", _error == null ? JSONObject.NULL : _error);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return obj.toString();
        }

        public static BackupResult fromJson(String json) throws JSONException {
            JSONObject obj = new JSONObject(json);
            long time = obj.getLong("time");
            String error = JsonUtils.optString(obj, "error");
            return new BackupResult(new Date(time), error);
        }
    }

    public enum CodeGrouping {

        HALVES(-1), NO_GROUPING(-2), GROUPING_TWOS(2), GROUPING_THREES(3), GROUPING_FOURS(4);

        private final int _value;

        CodeGrouping(int value) {
            _value = value;
        }

        public int getValue() {
            return _value;
        }
    }
}
