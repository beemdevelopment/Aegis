package me.impy.aegis.ui;

import android.os.Bundle;

import me.impy.aegis.R;

public class PreferencesActivity extends AegisActivity {
    private PreferencesFragment _fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _fragment = new PreferencesFragment();
        _fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(android.R.id.content, _fragment).commit();
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Default);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // pass permission request results to the fragment
        _fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
