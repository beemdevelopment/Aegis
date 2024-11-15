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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.BackupsVersioningStrategy;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.google.android.material.color.MaterialColors;

public class BackupsPreferencesFragment extends PreferencesFragment {
    private SwitchPreferenceCompat _androidBackupsPreference;
    private SwitchPreferenceCompat _backupsPreference;
    private SwitchPreferenceCompat _backupReminderPreference;
    private Preference _versioningStrategyPreference;
    private Preference _backupsLocationPreference;
    private Preference _backupsTriggerPreference;
    private Preference _backupsVersionsPreference;
    private Preference _backupsPasswordWarningPreference;

    private Preference _builtinBackupStatusPreference;
    private Preference _androidBackupStatusPreference;

    private final ActivityResultLauncher<Intent> backupsResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                Intent data = activityResult.getData();
                int resultCode = activityResult.getResultCode();
                if (data != null) {
                    onSelectBackupsLocationResult(resultCode, data);
                }
            });

    @Override
    public void onResume() {
        super.onResume();
        updateBackupPreference();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
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
                Dialogs.showBackupsVersioningStrategy(requireContext(), BackupsVersioningStrategy.MULTIPLE_BACKUPS, strategy -> {
                    if (strategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
                        selectBackupsLocation();
                    } else if (strategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
                        createBackupFile();
                    }
                });
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

        _versioningStrategyPreference = requirePreference("pref_versioning_strategy");
        updateBackupsVersioningStrategySummary();
        _versioningStrategyPreference.setOnPreferenceClickListener(preference -> {
            BackupsVersioningStrategy currentStrategy = _prefs.getBackupVersioningStrategy();
            Dialogs.showBackupsVersioningStrategy(requireContext(), currentStrategy, strategy -> {
                if (strategy == currentStrategy) {
                    return;
                }
                if (strategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
                    selectBackupsLocation();
                } else if (strategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
                    createBackupFile();
                }
            });
            return true;
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

        _backupsLocationPreference = requirePreference("pref_backups_location");
        updateBackupsLocationSummary();
        _backupsLocationPreference.setOnPreferenceClickListener(preference -> {
            BackupsVersioningStrategy currentStrategy = _prefs.getBackupVersioningStrategy();
            if (currentStrategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
                selectBackupsLocation();
            } else if (currentStrategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
                createBackupFile();
            }
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
        updateBackupsVersionsSummary();
        _backupsVersionsPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showBackupVersionsPickerDialog(requireContext(), _prefs.getBackupsVersionCount(), number -> {
                _prefs.setBackupsVersionCount(number);
                updateBackupsVersionsSummary();
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

    private void onSelectBackupsLocationResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        requireContext().getContentResolver().takePersistableUriPermission(data.getData(), flags);

        _prefs.setBackupsLocation(uri);
        _prefs.setIsBackupsEnabled(true);
        updateBackupPreference();
        scheduleBackup();
        updateBackupsVersioningStrategySummary();
        updateBackupsLocationSummary();
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
        _versioningStrategyPreference.setVisible(backupEnabled);
        _backupsLocationPreference.setVisible(backupEnabled);
        _backupsTriggerPreference.setVisible(backupEnabled);
        _backupsVersionsPreference.setVisible(backupEnabled && _prefs.getBackupVersioningStrategy() != BackupsVersioningStrategy.SINGLE_BACKUP);
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
            pref.setIcon(R.drawable.ic_outline_error_24);
        } else if (res != null) {
            pref.setIcon(R.drawable.ic_outline_check_24);
        } else {
            pref.setIcon(null);
        }
    }

    private CharSequence getBackupStatusMessage(@Nullable Preferences.BackupResult res) {
        String message;
        int colorAttr = com.google.android.material.R.attr.colorError;
        if (res == null) {
            message = getString(R.string.backup_status_none);
        } else if (res.isSuccessful()) {
            colorAttr = R.attr.colorSuccess;
            message = getString(R.string.backup_status_success, res.getElapsedSince(requireContext()));
        } else {
            message = getString(R.string.backup_status_failed, res.getElapsedSince(requireContext()));
        }

        int color = MaterialColors.getColor(requireContext(), colorAttr, getClass().getCanonicalName());
        Spannable spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(color), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private void createBackupFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, VaultBackupManager.FILENAME_SINGLE);
        _vaultManager.fireIntentLauncher(this, intent, backupsResultLauncher);
    }

    private void selectBackupsLocation() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        _vaultManager.fireIntentLauncher(this, intent, backupsResultLauncher);
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

    private void updateBackupsVersioningStrategySummary() {
        BackupsVersioningStrategy currentStrategy = _prefs.getBackupVersioningStrategy();
        if (currentStrategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
            _versioningStrategyPreference.setSummary(R.string.pref_backups_versioning_strategy_keep_x_versions);
        } else if (currentStrategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
            _versioningStrategyPreference.setSummary(R.string.pref_backups_versioning_strategy_single_backup);
        }
    }

    private void updateBackupsLocationSummary() {
        Uri backupsLocation = _prefs.getBackupsLocation();
        BackupsVersioningStrategy currentStrategy = _prefs.getBackupVersioningStrategy();
        String text;
        if (currentStrategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
            text = getString(R.string.pref_backups_location_summary);
        } else if (currentStrategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
            text = getString(R.string.pref_backup_location_summary);
        } else {
            return;
        }
        String summary = String.format("%s: %s", text, Uri.decode(backupsLocation.toString()));
        _backupsLocationPreference.setSummary(summary);
    }

    private void updateBackupsVersionsSummary() {
        int count = _prefs.getBackupsVersionCount();
        if (count == Preferences.BACKUPS_VERSIONS_INFINITE) {
            _backupsVersionsPreference.setSummary(R.string.pref_backups_versions_infinite_summary);
        } else {
            String summary = getResources().getQuantityString(R.plurals.pref_backups_versions_summary, count, count);
            _backupsVersionsPreference.setSummary(summary);
        }
    }
}
