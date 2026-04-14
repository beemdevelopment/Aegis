package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BehaviorPreferencesFragment extends PreferencesFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_behavior);

        Preference currentSearchBehaviorPreference = requirePreference("pref_search_behavior");
        currentSearchBehaviorPreference.setSummary(getSearchBehaviorSummary());
        currentSearchBehaviorPreference.setOnPreferenceClickListener((preference) -> {
            final int[] items = Preferences.SEARCH_BEHAVIOR_SETTINGS;
            final String[] textItems = getResources().getStringArray(R.array.pref_search_behavior_types);
            final boolean[] checkedItems = new boolean[items.length];
            for (int i = 0; i < items.length; i++) {
                checkedItems[i] = _prefs.isSearchBehaviorTypeEnabled(items[i]);
            }

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_search_behavior_prompt)
                    .setMultiChoiceItems(textItems, checkedItems, (dialog, index, isChecked) -> {
                        checkedItems[index] = isChecked;

                        boolean containsAtLeastOneCheckedItem = false;
                        for(boolean b: checkedItems) {
                            if (b) {
                                containsAtLeastOneCheckedItem = true;
                                break;
                            }
                        }

                        AlertDialog alertDialog = (AlertDialog) dialog;
                        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                        positiveButton.setEnabled(containsAtLeastOneCheckedItem);
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        int searchBehavior = 0;
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                searchBehavior |= items[i];
                            }
                        }

                        _prefs.setSearchBehaviorMask(searchBehavior);
                        currentSearchBehaviorPreference.setSummary(getSearchBehaviorSummary());
                    })
                    .setNegativeButton(android.R.string.cancel, null);

            Dialogs.showSecureDialog(builder.create());
            return true;
        });

        Preference entryPausePreference = requirePreference("pref_pause_entry");
        entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || _prefs.isEntryHighlightEnabled());

        Preference entryHighlightPreference = requirePreference("pref_highlight_entry");
        entryHighlightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || (boolean) newValue);
            return true;
        });

        Preference tapActions = findPreference("pref_tap_actions");
        if (tapActions != null) {
            tapActions.setOnPreferenceClickListener(preference -> {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content, new TapActionsPreferencesFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            });
        }
    }

    private String getSearchBehaviorSummary() {
        final int[] settings = Preferences.SEARCH_BEHAVIOR_SETTINGS;
        final String[] descriptions = getResources().getStringArray(R.array.pref_search_behavior_types);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < settings.length; i++) {
            if (_prefs.isSearchBehaviorTypeEnabled(settings[i])) {
                if (builder.length() != 0) {
                    builder.append(", ");
                }

                builder.append(descriptions[i].toLowerCase());
            }
        }

        return getString(R.string.pref_search_behavior_summary, builder.toString());
    }
}
