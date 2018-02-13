package me.impy.aegis;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class PreferencesActivity extends AegisActivity {
    public static final int ACTION_IMPORT = 0;
    public static final int ACTION_EXPORT = 1;
    public static final int ACTION_SLOTS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesFragment fragment = new PreferencesFragment();
        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Default);
        }
    }

    public static class PreferencesFragment extends PreferenceFragment {
        private Intent _result = new Intent();

        private void setResult() {
            getActivity().setResult(RESULT_OK, _result);
        }

        private void finish() {
            setResult();
            getActivity().finish();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // set the result intent in advance
            setResult();

            Preference nightModePreference = findPreference("pref_night_mode");
            nightModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(), "Night mode will be enabled after closing this screen", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            Preference exportPreference = findPreference("pref_import");
            exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    _result.putExtra("action", ACTION_IMPORT);
                    finish();
                    return true;
                }
            });

            Preference importPreference = findPreference("pref_export");
            importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    _result.putExtra("action", ACTION_EXPORT);
                    finish();
                    return true;
                }
            });

            Preference slotsPreference = findPreference("pref_slots");
            slotsPreference.setEnabled(getArguments().getBoolean("encrypted"));
            slotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    _result.putExtra("action", ACTION_SLOTS);
                    finish();
                    return true;
                }
            });

            Preference issuerPreference = findPreference("pref_issuer");
            issuerPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    _result.putExtra("needsRefresh", true);
                    return true;
                }
            });
        }
    }
}
