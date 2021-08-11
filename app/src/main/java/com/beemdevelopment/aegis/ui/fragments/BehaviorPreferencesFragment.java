package com.beemdevelopment.aegis.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;

public class BehaviorPreferencesFragment extends PreferencesFragment {

    private Preferences _prefs;
    private Preference _entryPausePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_behavior);

        _prefs = getPreferences();

        Preference copyOnTapPreference = findPreference("pref_copy_on_tap");
        copyOnTapPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        Preference entryHighlightPreference = findPreference("pref_highlight_entry");
        entryHighlightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            _entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || (boolean) newValue);
            return true;
        });

        _entryPausePreference = findPreference("pref_pause_entry");
        _entryPausePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });
        _entryPausePreference.setEnabled(_prefs.isTapToRevealEnabled() || _prefs.isEntryHighlightEnabled());
    }
}
