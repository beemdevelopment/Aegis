package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class PreferencesActivity extends AegisActivity {
    private PreferencesFragment _fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            _fragment = new PreferencesFragment();
            _fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, _fragment).commit();
        } else {
            _fragment = (PreferencesFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String preference = intent.getStringExtra("pref");
        if (preference != null) {
            _fragment.scrollToPreference(preference);
            intent.removeExtra("pref");
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
