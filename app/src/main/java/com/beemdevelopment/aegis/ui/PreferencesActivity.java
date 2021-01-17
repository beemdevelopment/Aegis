package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.fragments.MainPreferencesFragment;
import com.beemdevelopment.aegis.ui.fragments.PreferencesFragment;

public class PreferencesActivity extends AegisActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private PreferencesFragment _fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        setSupportActionBar(findViewById(R.id.toolbar));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            _fragment = new MainPreferencesFragment();
            _fragment.setArguments(getIntent().getExtras());

             getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, _fragment)
                    .commit();

            PreferencesFragment requestedFragment = getRequestedFragment();
            if (requestedFragment != null) {
                _fragment = requestedFragment;
                showFragment(_fragment);
            }
        } else {
            _fragment = (PreferencesFragment) getSupportFragmentManager().findFragmentById(R.id.content);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setTitle(R.string.action_settings);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        _fragment = (PreferencesFragment) getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        _fragment.setArguments(pref.getExtras());
        _fragment.setTargetFragment(caller, 0);
        showFragment(_fragment);

        setTitle(pref.getTitle());
        return true;
    }

    private void showFragment(PreferencesFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressWarnings("unchecked")
    private PreferencesFragment getRequestedFragment() {
        Class<? extends PreferencesFragment> fragmentType = (Class<? extends PreferencesFragment>) getIntent().getSerializableExtra("fragment");
        if (fragmentType == null) {
            return null;
        }

        try {
            return fragmentType.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
