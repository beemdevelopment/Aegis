package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.os.Bundle;

import com.beemdevelopment.aegis.R;

public class MainPreferencesFragment extends PreferencesFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}
