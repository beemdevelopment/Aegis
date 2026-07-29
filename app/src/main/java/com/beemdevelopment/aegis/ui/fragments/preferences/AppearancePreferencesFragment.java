package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.AccountNamePosition;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.ui.GroupManagerActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

public class AppearancePreferencesFragment extends PreferencesFragment {
    private Preference _groupsPreference;
    private Preference _resetUsageCountPreference;
    private Preference _currentAccountNamePositionPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_appearance);

        _groupsPreference = requirePreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(requireContext(), GroupManagerActivity.class);
            startActivity(intent);
            return true;
        });

        _resetUsageCountPreference = requirePreference("pref_reset_usage_count");
        _resetUsageCountPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.preference_reset_usage_count)
                    .setMessage(R.string.preference_reset_usage_count_dialog)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> _prefs.clearUsageCount())
                    .setNegativeButton(android.R.string.no, null)
                    .create());
            return true;
        });

        int currentTheme = _prefs.getCurrentTheme().ordinal();
        Preference darkModePreference = requirePreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(preference -> {
            int currentTheme1 = _prefs.getCurrentTheme().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_theme)
                    .setSingleChoiceItems(R.array.theme_titles, currentTheme1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentTheme(Theme.fromInteger(i));

                        dialog.dismiss();

                        requireActivity().recreate();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference dynamicColorsPreference = requirePreference("pref_dynamic_colors");
        dynamicColorsPreference.setEnabled(DynamicColors.isDynamicColorAvailable());
        dynamicColorsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        Preference langPreference = requirePreference("pref_lang");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            String[] langs = getResources().getStringArray(R.array.pref_lang_values);
            String[] langNames = getResources().getStringArray(R.array.pref_lang_entries);
            List<String> langList = Arrays.asList(langs);
            int curLangIndex = langList.contains(_prefs.getLanguage()) ? langList.indexOf(_prefs.getLanguage()) : 0;
            langPreference.setSummary(langNames[curLangIndex]);
            langPreference.setOnPreferenceClickListener(preference -> {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.pref_lang_title)
                        .setSingleChoiceItems(langNames, curLangIndex, (dialog, which) -> {
                            int newLangIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            _prefs.setLanguage(langs[newLangIndex]);
                            langPreference.setSummary(langNames[newLangIndex]);

                            dialog.dismiss();

                            requireActivity().recreate();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create());
                return true;
            });
        } else {
            // Setting locale doesn't work on Marshmallow or below
            langPreference.setVisible(false);
        }

        int currentViewMode = _prefs.getCurrentViewMode().ordinal();
        Preference viewModePreference = requirePreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(preference -> {
            int currentViewMode1 = _prefs.getCurrentViewMode().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_view_mode)
                    .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentViewMode(ViewMode.fromInteger(i));
                        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                        refreshAccountNamePositionText();
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference showExpirationStatePreference = requirePreference("pref_expiration_state");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showExpirationStatePreference.setSummary(getString(R.string.pref_expiration_state_fallback));
        }

        String[] codeGroupings = getResources().getStringArray(R.array.pref_code_groupings_values);
        String[] codeGroupingNames = getResources().getStringArray(R.array.pref_code_groupings);
        Preference codeDigitGroupingPreference = requirePreference("pref_code_group_size_string");
        codeDigitGroupingPreference.setOnPreferenceClickListener(preference -> {
            int currentCodeGroupingIndex = Arrays.asList(codeGroupings).indexOf(_prefs.getCodeGroupSize().name());

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_code_group_size_title)
                    .setSingleChoiceItems(codeGroupingNames, currentCodeGroupingIndex, (dialog, which) -> {
                        int newCodeGroupingIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCodeGroupSize(Preferences.CodeGrouping.valueOf(codeGroupings[newCodeGroupingIndex]));

                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());
            return true;
        });

        int currentAccountNamePosition = _prefs.getAccountNamePosition().ordinal();
        _currentAccountNamePositionPreference = requirePreference("pref_account_name_position");
        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[currentAccountNamePosition]));
        _currentAccountNamePositionPreference.setOnPreferenceClickListener(preference -> {
            int currentAccountNamePosition1 = _prefs.getAccountNamePosition().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.choose_account_name_position))
                    .setSingleChoiceItems(R.array.account_name_position_titles, currentAccountNamePosition1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setAccountNamePosition(AccountNamePosition.fromInteger(i));
                        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[i]));
                        refreshAccountNamePositionText();
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        refreshAccountNamePositionText();
    }

    private void refreshAccountNamePositionText() {
        boolean override = (_prefs.getCurrentViewMode() == ViewMode.TILES && _prefs.getAccountNamePosition() == AccountNamePosition.END);

        if (override) {
            _currentAccountNamePositionPreference.setSummary(String.format("%s: %s. %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[_prefs.getAccountNamePosition().ordinal()], getString(R.string.pref_account_name_position_summary_override)));
        } else {
            _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[_prefs.getAccountNamePosition().ordinal()]));
        }
    }
}
