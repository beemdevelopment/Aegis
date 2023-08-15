package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.CopyBehavior;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

public class BehaviorPreferencesFragment extends PreferencesFragment {
    private Preference _entryPausePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_behavior);

        int currentCopyBehavior = _prefs.getCopyBehavior().ordinal();
        Preference copyBehaviorPreference = requirePreference("pref_copy_behavior");
        copyBehaviorPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.copy_behavior_titles)[currentCopyBehavior]));
        copyBehaviorPreference.setOnPreferenceClickListener(preference -> {
            int currentCopyBehavior1 = _prefs.getCopyBehavior().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.choose_copy_behavior))
                    .setSingleChoiceItems(R.array.copy_behavior_titles, currentCopyBehavior1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCopyBehavior(CopyBehavior.fromInteger(i));
                        copyBehaviorPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.copy_behavior_titles)[i]));
                        getResult().putExtra("needsRefresh", true);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference entryHighlightPreference = requirePreference("pref_highlight_entry");
        entryHighlightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            _entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || (boolean) newValue);
            return true;
        });

        _entryPausePreference = requirePreference("pref_pause_entry");
        _entryPausePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });
        _entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || _prefs.isEntryHighlightEnabled());
    }
}
