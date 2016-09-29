package me.impy.aegis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

public class PreferencesActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(mySharedPreferences.getBoolean("pref_night_mode", false))
        {
            setTheme(R.style.AppTheme_Dark);
        } else
        {
            setTheme(R.style.AppTheme_Default);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();

    }
    public static class PreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            final Preference nightModePreference = findPreference("pref_night_mode");
            nightModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(), "Night mode will be enabled after closing this screen", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }
}
