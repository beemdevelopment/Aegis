package me.impy.aegis.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import me.impy.aegis.AegisApplication;

public abstract class AegisActivity extends AppCompatActivity {
    private AegisApplication _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        // set FLAG_SECURE on the window of every AegisActivity
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // set the theme
        boolean darkMode = _app.getPreferences().getBoolean("pref_dark_mode", false);
        setPreferredTheme(darkMode);
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected abstract void setPreferredTheme(boolean darkMode);
}
