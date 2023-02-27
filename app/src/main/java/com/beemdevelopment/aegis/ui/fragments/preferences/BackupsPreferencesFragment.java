package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;

public class BackupsPreferencesFragment extends PreferencesFragment {
    private SwitchPreferenceCompat _androidBackupsPreference;
    private SwitchPreferenceCompat _backupsPreference;
    private SwitchPreferenceCompat _backupReminderPreference;
    private Preference _backupsLocationPreference;
    private Preference _backupsTriggerPreference;
    private Preference _backupsVersionsPreference;
    private Preference _backupsPasswordWarningPreference;

    private Preference _builtinBackupStatusPreference;
    private Preference _androidBackupStatusPreference;

    @Override
    public void onResume() {
        super.onResume();
        updateBackupPreference();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_backups);

        _backupsPasswordWarningPreference = requirePreference("pref_backups_warning_password");
        _builtinBackupStatusPreference = requirePreference("pref_status_backup_builtin");
        _builtinBackupStatusPreference.setOnPreferenceClickListener(preference -> {
            Preferences.BackupResult backupRes = _prefs.getBuiltInBackupResult();
            if (backupRes != null && !backupRes.isSuccessful()) {
                Dialogs.showBackupErrorDialog(requireContext(), backupRes, null);
            }
            return true;
        });
        _androidBackupStatusPreference = requirePreference("pref_status_backup_android");
        _androidBackupStatusPreference.setOnPreferenceClickListener(preference -> {
            Preferences.BackupResult backupRes = _prefs.getAndroidBackupResult();
            if (backupRes != null && !backupRes.isSuccessful()) {
                Dialogs.showBackupErrorDialog(requireContext(), backupRes, null);
            }
            return true;
        });

        _backupsPreference = requirePreference("pref_backups");
        _backupsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                selectBackupsLocation();
            } else {
                _prefs.setIsBackupsEnabled(false);
                updateBackupPreference();
            }

            return false;
        });

        _backupReminderPreference = requirePreference("pref_backup_reminder");
        _backupReminderPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(boolean)newValue) {
                Dialogs.showCheckboxDialog(getContext(), R.string.pref_backups_reminder_dialog_title,
                        R.string.pref_backups_reminder_dialog_summary,
                        R.string.understand_risk_accept,
                        this::saveAndDisableBackupReminder
                );
            } else {
                _prefs.setIsBackupReminderEnabled(true);
                return true;
            }

            return false;
        });

        _androidBackupsPreference = requirePreference("pref_android_backups");
        _androidBackupsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _prefs.setIsAndroidBackupsEnabled((boolean) newValue);
            updateBackupPreference();
            if ((boolean) newValue) {
                _vaultManager.scheduleAndroidBackup();
            }
            return false;
        });

        Uri backupLocation = _prefs.getBackupsLocation();
        _backupsLocationPreference = requirePreference("pref_backups_location");
        if (backupLocation != null) {
            _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(backupLocation.toString())));
        }
        _backupsLocationPreference.setOnPreferenceClickListener(preference -> {
            selectBackupsLocation();
            return false;
        });

        _backupsTriggerPreference = requirePreference("pref_backups_trigger");
        _backupsTriggerPreference.setOnPreferenceClickListener(preference -> {
            if (_prefs.isBackupsEnabled()) {
                scheduleBackup();
                _builtinBackupStatusPreference.setVisible(false);
            }
            return true;
        });

        _backupsVersionsPreference = requirePreference("pref_backups_versions");
        _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, _prefs.getBackupsVersionCount(), _prefs.getBackupsVersionCount()));
        _backupsVersionsPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showBackupVersionsPickerDialog(requireContext(), _prefs.getBackupsVersionCount(), number -> {
                number = number * 5 + 5;
                _prefs.setBackupsVersionCount(number);
                _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, _prefs.getBackupsVersionCount(), _prefs.getBackupsVersionCount()));
            });
            return false;
        });
    }

    private void saveAndDisableBackupReminder(boolean understand) {
        if (understand) {
            _prefs.setIsBackupReminderEnabled(false);
            updateBackupPreference();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && requestCode == CODE_BACKUPS) {
            onSelectBackupsLocationResult(resultCode, data);
        }
    }

    private void onSelectBackupsLocationResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        requireContext().getContentResolver().takePersistableUriPermission(data.getData(), flags);

        _prefs.setBackupsLocation(uri);
        _prefs.setIsBackupsEnabled(true);
        _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(uri.toString())));
        updateBackupPreference();
        scheduleBackup();
    }

    private void updateBackupPreference() {
        boolean encrypted = _vaultManager.getVault().isEncryptionEnabled();
        boolean androidBackupEnabled = _prefs.isAndroidBackupsEnabled() && encrypted;
        boolean backupEnabled = _prefs.isBackupsEnabled() && encrypted;
        boolean backupReminderEnabled = _prefs.isBackupReminderEnabled();
        _backupsPasswordWarningPreference.setVisible(_vaultManager.getVault().isBackupPasswordSet());
        _androidBackupsPreference.setChecked(androidBackupEnabled);
        _androidBackupsPreference.setEnabled(encrypted);
        _backupsPreference.setChecked(backupEnabled);
        _backupsPreference.setEnabled(encrypted);
        _backupReminderPreference.setChecked(backupReminderEnabled);
        _backupsLocationPreference.setVisible(backupEnabled);
        _backupsTriggerPreference.setVisible(backupEnabled);
        _backupsVersionsPreference.setVisible(backupEnabled);
        if (backupEnabled) {
            updateBackupStatus(_builtinBackupStatusPreference, _prefs.getBuiltInBackupResult());
        }
        if (androidBackupEnabled) {
            updateBackupStatus(_androidBackupStatusPreference, _prefs.getAndroidBackupResult());
        }
        _builtinBackupStatusPreference.setVisible(backupEnabled);
        _androidBackupStatusPreference.setVisible(androidBackupEnabled);
    }

    private void updateBackupStatus(Preference pref, Preferences.BackupResult res) {
        boolean backupFailed = res != null && !res.isSuccessful();
        pref.setSummary(getBackupStatusMessage(res));
        pref.setSelectable(backupFailed);

        // TODO: Find out why setting the tint of the icon doesn't work
        if (backupFailed) {
            pref.setIcon(R.drawable.ic_info_outline_black_24dp);
        } else if (res != null) {
            pref.setIcon(R.drawable.ic_check_black_24dp);
        } else {
            pref.setIcon(null);
        }
    }

    private CharSequence getBackupStatusMessage(@Nullable Preferences.BackupResult res) {
        String message;
        int color = R.color.warning_color;
        if (res == null) {
            message = getString(R.string.backup_status_none);
        } else if (res.isSuccessful()) {
            color = R.color.success_color;
            message = getString(R.string.backup_status_success, res.getElapsedSince(requireContext()));
        } else {
            message = getString(R.string.backup_status_failed, res.getElapsedSince(requireContext()));
        }

        Spannable spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(color)), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (color == R.color.warning_color) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    private void selectBackupsLocation() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        _vaultManager.startActivityForResult(this, intent, CODE_BACKUPS);
    }

    private void scheduleBackup() {
        try {
            _vaultManager.scheduleBackup();
            Toast.makeText(requireContext(), R.string.backup_successful, Toast.LENGTH_LONG).show();
        } catch (VaultRepositoryException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(requireContext(), R.string.backup_error, e);
        }
    }
}
