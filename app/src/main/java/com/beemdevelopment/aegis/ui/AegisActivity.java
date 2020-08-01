package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.AppCompatActivity;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.vault.VaultManagerException;

import java.util.Locale;
import java.util.Map;

public abstract class AegisActivity extends AppCompatActivity implements AegisApplication.LockListener {
    private boolean _resumed;
    private AegisApplication _app;
    private Theme _configuredTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _app = (AegisApplication) getApplication();

        // set the theme and locale before creating the activity
        Preferences prefs = getPreferences();
        onSetTheme();
        setLocale(prefs.getLocale());
        super.onCreate(savedInstanceState);

        // if the app was killed, relaunch MainActivity and close everything else
        if (savedInstanceState != null && isOrphan()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        // set FLAG_SECURE on the window of every AegisActivity
        if (getPreferences().isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // register a callback to listen for lock events
        _app.registerLockListener(this);
    }

    @Override
    protected void onDestroy() {
        _app.unregisterLockListener(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _resumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        _resumed = false;
    }

    @CallSuper
    @Override
    public void onLocked() {
        if (isOrphan()) {
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected Preferences getPreferences() {
        return _app.getPreferences();
    }

    /**
     * Called when the activity is expected to set its theme.
     */
    protected void onSetTheme() {
        setTheme(ThemeMap.DEFAULT);
    }

    /**
     * Sets the theme of the activity. The actual style that is set is picked from the
     * given map, based on the theme configured by the user.
     */
    protected void setTheme(Map<Theme, Integer> themeMap) {
        int theme = themeMap.get(getConfiguredTheme());
        setTheme(theme);
    }

    protected Theme getConfiguredTheme() {
        Theme theme = getPreferences().getCurrentTheme();

        if (theme == Theme.SYSTEM || theme == Theme.SYSTEM_AMOLED) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    theme = Theme.LIGHT;
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    theme = theme == Theme.SYSTEM_AMOLED ? Theme.AMOLED : Theme.DARK;
                    break;
            }
        }

        return theme;
    }

    protected void setLocale(Locale locale) {
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        this.getResources().updateConfiguration(config, this.getResources().getDisplayMetrics());
    }

    protected boolean saveVault(boolean backup) {
        try {
            getApp().getVaultManager().save(backup);
            return true;
        } catch (VaultManagerException e) {
            Toast.makeText(this, getString(R.string.saving_error), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Reports whether this Activity has been resumed. (i.e. onResume was called)
     */
    protected boolean isOpen() {
        return _resumed;
    }

    /**
     * Reports whether this Activity instance has become an orphan. This can happen if
     * the vault was locked by an external trigger while the Activity was still open.
     */
    protected boolean isOrphan() {
        return !(this instanceof MainActivity) && !(this instanceof AuthActivity) && _app.isVaultLocked();
    }
}
