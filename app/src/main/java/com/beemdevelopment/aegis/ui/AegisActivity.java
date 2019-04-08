package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;

import androidx.appcompat.app.AppCompatActivity;

public abstract class AegisActivity extends AppCompatActivity {
    private AegisApplication _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        // if the app was killed, relaunch MainActivity and close everything else
        if (!(this instanceof MainActivity) && !(this instanceof AuthActivity) && _app.getDatabaseManager().isLocked()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        // set FLAG_SECURE on the window of every AegisActivity
        if (getPreferences().isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // set the theme
        setPreferredTheme(Theme.fromInteger(getPreferences().getCurrentTheme()));
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected Preferences getPreferences() {
        return _app.getPreferences();
    }

    protected void setPreferredTheme(Theme theme) {
        switch (theme) {
            case LIGHT:
                setTheme(R.style.AppTheme);
                break;
            case DARK:
                setTheme(R.style.AppTheme_Dark);
                break;
            case AMOLED:
                setTheme(R.style.AppTheme_TrueBlack);
                break;
        }
    }
}
