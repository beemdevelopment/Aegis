package me.impy.aegis.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.impy.aegis.AegisApplication;

public abstract class AegisActivity extends AppCompatActivity {
    private AegisApplication _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        boolean nightMode = _app.getPreferences().getBoolean("pref_night_mode", false);
        setPreferredTheme(nightMode);
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected abstract void setPreferredTheme(boolean nightMode);
}
