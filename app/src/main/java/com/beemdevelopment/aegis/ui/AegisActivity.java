package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;

public abstract class AegisActivity extends AppCompatActivity {
    private AegisApplication _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        // set FLAG_SECURE on the window of every AegisActivity
        if (getPreferences().isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // set the theme
        setPreferredTheme(getPreferences().isDarkModeEnabled());
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected Preferences getPreferences() {
        return _app.getPreferences();
    }

    protected void setPreferredTheme(boolean darkMode) {
        if (darkMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
    }
}
