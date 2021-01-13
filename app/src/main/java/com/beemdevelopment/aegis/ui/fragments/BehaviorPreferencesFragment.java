package com.beemdevelopment.aegis.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import com.beemdevelopment.aegis.R;

public class BehaviorPreferencesFragment extends PreferencesFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_behavior);

        Preference copyOnTapPreference = findPreference("pref_copy_on_tap");
        copyOnTapPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        Preference entryHighlightPreference = findPreference("pref_highlight_entry");
        entryHighlightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });
    }
}
