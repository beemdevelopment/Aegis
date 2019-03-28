package com.beemdevelopment.aegis.ui;

import android.os.Bundle;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;

public class PreferencesActivity extends AegisActivity {
    private PreferencesFragment _fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _fragment = new PreferencesFragment();
        _fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, _fragment).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // pass permission request results to the fragment
        _fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle inState) {
        // pass the stored result intent back to the fragment
        if (inState.containsKey("result")) {
            _fragment.setResult(inState.getParcelable("result"));
        }
        super.onRestoreInstanceState(inState);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        // save the result intent of the fragment
        // this is done so we don't lose anything if the fragment calls recreate on this activity
        outState.putParcelable("result", _fragment.getResult());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void setPreferredTheme(Theme theme) {
        switch (theme) {
            case LIGHT:
                setTheme(R.style.AppTheme);
                break;

            case DARK:
                setTheme(R.style.AppTheme_Dark);
                break;

            case AMOLED:
                setTheme(R.style.AppTheme_TrueBlack_Preferences);
                break;
        }
    }
}
