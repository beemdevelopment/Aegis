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
    protected void setPreferredTheme(boolean darkMode) {
        if (darkMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // pass permission request results to the fragment
        _fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected final void onRestoreInstanceState(final Bundle inState) {
        // pass the stored result intent back to the fragment
        if (inState.containsKey("result")) {
            _fragment.setResult(inState.getParcelable("result"));
        }
        super.onRestoreInstanceState(inState);
    }

    @Override
    protected final void onSaveInstanceState(final Bundle outState) {
        // save the result intent of the fragment
        // this is done so we don't lose anything if the fragment calls recreate on this activity
        outState.putParcelable("result", _fragment.getResult());
        super.onSaveInstanceState(outState);
    }
}
