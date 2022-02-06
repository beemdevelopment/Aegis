package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.ui.GroupManagerActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.ArrayList;
import java.util.HashSet;

public class AppearancePreferencesFragment extends PreferencesFragment {
    private Preference _groupsPreference;
    private Preference _resetUsageCountPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_appearance);

        _groupsPreference = findPreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), GroupManagerActivity.class);
            intent.putExtra("groups", new ArrayList<>(_vaultManager.getVault().getGroups()));
            startActivityForResult(intent, CODE_GROUPS);
            return true;
        });

        _resetUsageCountPreference = findPreference("pref_reset_usage_count");
        _resetUsageCountPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.preference_reset_usage_count)
                    .setMessage(R.string.preference_reset_usage_count_dialog)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> _prefs.clearUsageCount())
                    .setNegativeButton(android.R.string.no, null)
                    .create());
            return true;
        });

        int currentTheme = _prefs.getCurrentTheme().ordinal();
        Preference darkModePreference = findPreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(preference -> {
            int currentTheme1 = _prefs.getCurrentTheme().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.choose_theme)
                    .setSingleChoiceItems(R.array.theme_titles, currentTheme1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentTheme(Theme.fromInteger(i));

                        dialog.dismiss();

                        getResult().putExtra("needsRecreate", true);
                        getActivity().recreate();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference langPreference = findPreference("pref_lang");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            langPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                getResult().putExtra("needsRecreate", true);
                getActivity().recreate();
                return true;
            });
        } else {
            // Setting locale doesn't work on Marshmallow or below
            langPreference.setVisible(false);
        }

        int currentViewMode = _prefs.getCurrentViewMode().ordinal();
        Preference viewModePreference = findPreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(preference -> {
            int currentViewMode1 = _prefs.getCurrentViewMode().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.choose_view_mode)
                    .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentViewMode(ViewMode.fromInteger(i));
                        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                        getResult().putExtra("needsRefresh", true);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference codeDigitGroupingPreference = findPreference("pref_code_group_size");
        codeDigitGroupingPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        Preference issuerPreference = findPreference("pref_account_name");
        issuerPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && requestCode == CODE_GROUPS) {
            onGroupManagerResult(resultCode, data);
        }
    }

    private void onGroupManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        HashSet<String> groups = new HashSet<>(data.getStringArrayListExtra("groups"));

        for (VaultEntry entry : _vaultManager.getVault().getEntries()) {
            if (!groups.contains(entry.getGroup())) {
                entry.setGroup(null);
            }
        }

        saveAndBackupVault();
    }
}
