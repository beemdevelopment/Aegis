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

            Preference nightModePreference = findPreference("pref_night_mode");
            nightModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Intent i = new Intent(getActivity(), MainActivity.class);
                    //startActivity(i);
                    return false;
                }
            });
        }
    }
}
