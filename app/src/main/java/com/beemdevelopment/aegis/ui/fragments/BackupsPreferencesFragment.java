package com.beemdevelopment.aegis.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.AegisActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultManagerException;

public class BackupsPreferencesFragment extends PreferencesFragment {
    private SwitchPreferenceCompat _androidBackupsPreference;
    private SwitchPreferenceCompat _backupsPreference;
    private Preference _backupsLocationPreference;
    private Preference _backupsTriggerPreference;
    private Preference _backupsVersionsPreference;

    @Override
    public void onResume() {
        super.onResume();
        updateBackupPreference();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_backups);
        Preferences prefs = getPreferences();

        _backupsPreference = findPreference("pref_backups");
        _backupsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                selectBackupsLocation();
            } else {
                prefs.setIsBackupsEnabled(false);
                updateBackupPreference();
            }

            return false;
        });

        _androidBackupsPreference = findPreference("pref_android_backups");
        _androidBackupsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            prefs.setIsAndroidBackupsEnabled((boolean) newValue);
            updateBackupPreference();
            getVault().androidBackupDataChanged();
            return false;
        });

        Uri backupLocation = prefs.getBackupsLocation();
        _backupsLocationPreference = findPreference("pref_backups_location");
        if (backupLocation != null) {
            _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(backupLocation.toString())));
        }
        _backupsLocationPreference.setOnPreferenceClickListener(preference -> {
            selectBackupsLocation();
            return false;
        });

        _backupsTriggerPreference = findPreference("pref_backups_trigger");
        _backupsTriggerPreference.setOnPreferenceClickListener(preference -> {
            if (prefs.isBackupsEnabled()) {
                try {
                    getVault().backup();
                    Toast.makeText(getActivity(), R.string.backup_successful, Toast.LENGTH_LONG).show();
                } catch (VaultManagerException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(getContext(), R.string.backup_error, e);
                }
            }
            return true;
        });

        _backupsVersionsPreference = findPreference("pref_backups_versions");
        _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, prefs.getBackupsVersionCount(), prefs.getBackupsVersionCount()));
        _backupsVersionsPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showBackupVersionsPickerDialog(getActivity(), number -> {
                number = number * 5 + 5;
                prefs.setBackupsVersionCount(number);
                _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, prefs.getBackupsVersionCount(), prefs.getBackupsVersionCount()));
            });
            return false;
        });
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
        getContext().getContentResolver().takePersistableUriPermission(data.getData(), flags);

        Preferences prefs = getPreferences();
        prefs.setBackupsLocation(uri);
        prefs.setIsBackupsEnabled(true);
        prefs.setBackupsError(null);
        _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(uri.toString())));
        updateBackupPreference();
    }

    private void updateBackupPreference() {
        boolean encrypted = getVault().isEncryptionEnabled();
        boolean androidBackupEnabled = getPreferences().isAndroidBackupsEnabled() && encrypted;
        boolean backupEnabled = getPreferences().isBackupsEnabled() && encrypted;
        _androidBackupsPreference.setChecked(androidBackupEnabled);
        _androidBackupsPreference.setEnabled(encrypted);
        _backupsPreference.setChecked(backupEnabled);
        _backupsPreference.setEnabled(encrypted);
        _backupsLocationPreference.setVisible(backupEnabled);
        _backupsTriggerPreference.setVisible(backupEnabled);
        _backupsVersionsPreference.setVisible(backupEnabled);
    }

    private void selectBackupsLocation() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        AegisActivity.Helper.startExtActivityForResult(this, intent, CODE_BACKUPS);
    }
}
