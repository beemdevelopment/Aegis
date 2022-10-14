package com.beemdevelopment.aegis.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.ViewPropertyAnimatorCompat;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.helpers.ThemeHelper;
import com.beemdevelopment.aegis.icons.IconPackManager;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.google.android.material.color.DynamicColors;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.InstallIn;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EarlyEntryPoint;
import dagger.hilt.android.EarlyEntryPoints;
import dagger.hilt.components.SingletonComponent;

@AndroidEntryPoint
public abstract class AegisActivity extends AppCompatActivity implements VaultManager.LockListener {
    protected Preferences _prefs;

    @Inject
    protected VaultManager _vaultManager;

    @Inject
    protected IconPackManager _iconPackManager;

    private ActionModeStatusGuardHack _statusGuardHack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set the theme and locale before creating the activity
        _prefs = EarlyEntryPoints.get(getApplicationContext(), PrefEntryPoint.class).getPreferences();
        onSetTheme();
        setLocale(_prefs.getLocale());
        super.onCreate(savedInstanceState);

        _statusGuardHack = new ActionModeStatusGuardHack();

        // set FLAG_SECURE on the window of every AegisActivity
        if (_prefs.isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // register a callback to listen for lock events
        _vaultManager.registerLockListener(this);
    }

    @Override
    @CallSuper
    protected void onDestroy() {
        _vaultManager.unregisterLockListener(this);
        super.onDestroy();
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        _vaultManager.setBlockAutoLock(false);
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    public void onLocked(boolean userInitiated) {
        setResult(RESULT_CANCELED, null);

        try {
            // Call a private overload of the finish() method to prevent the app
            // from disappearing from the recent apps menu
            Method method = Activity.class.getDeclaredMethod("finish", int.class);
            method.setAccessible(true);
            method.invoke(this, 2); // FINISH_TASK_WITH_ACTIVITY = 2
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // On recent Android versions, the overload  of the finish() method
            // used above is no longer accessible
            finishAndRemoveTask();
        }
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

        if (_prefs.isDynamicColorsEnabled()) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
    }

    protected Theme getConfiguredTheme() {
        Theme theme = _prefs.getCurrentTheme();

        if (theme == Theme.SYSTEM || theme == Theme.SYSTEM_AMOLED) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                theme = theme == Theme.SYSTEM_AMOLED ? Theme.AMOLED : Theme.DARK;
            } else {
                theme = Theme.LIGHT;
            }
        }

        return theme;
    }

    protected void setLocale(Locale locale) {
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    protected boolean saveVault() {
        try {
            _vaultManager.save();
            return true;
        } catch (VaultRepositoryException e) {
            Toast.makeText(this, getString(R.string.saving_error), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    protected boolean saveAndBackupVault() {
        try {
            _vaultManager.saveAndBackup();
            return true;
        } catch (VaultRepositoryException e) {
            Toast.makeText(this, getString(R.string.saving_error), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Closes this activity if it has become an orphan (isOrphan() == true) and launches MainActivity.
     * @param savedInstanceState the bundle passed to onCreate.
     * @return whether to abort onCreate.
     */
    protected boolean abortIfOrphan(Bundle savedInstanceState) {
        if (savedInstanceState == null || !isOrphan()) {
            return false;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return true;
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        _statusGuardHack.apply(View.VISIBLE);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        _statusGuardHack.apply(View.GONE);
    }

    /**
     * When starting/finishing an action mode, forcefully cancel the fade in/out animation and
     * set the status bar color. This requires the abc_decor_view_status_guard colors to be set
     * to transparent.
     *
     * This should fix any inconsistencies between the color of the action bar and the status bar
     * when an action mode is active.
     */
    private class ActionModeStatusGuardHack {
        private Field _fadeAnimField;
        private Field _actionModeViewField;

        @ColorInt
        private final int _statusBarColor;

        private ActionModeStatusGuardHack() {
            _statusBarColor = getWindow().getStatusBarColor();

            try {
                _fadeAnimField = getDelegate().getClass().getDeclaredField("mFadeAnim");
                _fadeAnimField.setAccessible(true);
                _actionModeViewField = getDelegate().getClass().getDeclaredField("mActionModeView");
                _actionModeViewField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
            }
        }

        private void apply(int visibility) {
            if (_fadeAnimField == null || _actionModeViewField == null) {
                return;
            }

            ViewPropertyAnimatorCompat fadeAnim;
            ViewGroup actionModeView;
            try {
                fadeAnim = (ViewPropertyAnimatorCompat) _fadeAnimField.get(getDelegate());
                actionModeView = (ViewGroup) _actionModeViewField.get(getDelegate());
            } catch (IllegalAccessException e) {
                return;
            }

            if (fadeAnim == null || actionModeView == null) {
                return;
            }

            fadeAnim.cancel();

            actionModeView.setVisibility(visibility);
            actionModeView.setAlpha(visibility == View.VISIBLE ? 1f : 0f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int statusBarColor = visibility == View.VISIBLE
                        ? ThemeHelper.getThemeColor(com.google.android.material.R.attr.colorSurfaceContainer, getTheme())
                        : _statusBarColor;
                getWindow().setStatusBarColor(statusBarColor);
            }
        }
    }

    /**
     * Reports whether this Activity instance has become an orphan. This can happen if
     * the vault was killed/locked by an external trigger while the Activity was still open.
     */
    private boolean isOrphan() {
        return !(this instanceof MainActivity)
                && !(this instanceof AuthActivity)
                && !(this instanceof IntroActivity)
                && !_vaultManager.isVaultLoaded();
    }

    @EarlyEntryPoint
    @InstallIn(SingletonComponent.class)
    public interface PrefEntryPoint {
        Preferences getPreferences();
    }
}
