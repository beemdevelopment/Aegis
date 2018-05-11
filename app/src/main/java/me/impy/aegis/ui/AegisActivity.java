package me.impy.aegis.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import me.impy.aegis.AegisApplication;

public abstract class AegisActivity extends AppCompatActivity {
    private AegisApplication _app;
    private boolean _darkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        // set FLAG_SECURE on the window of every AegisActivity
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // set the theme
        _darkMode = _app.getPreferences().getBoolean("pref_dark_mode", false);
        setPreferredTheme(_darkMode);
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected boolean isDark() {
        return _darkMode;
    }

    protected abstract void setPreferredTheme(boolean darkMode);
}
