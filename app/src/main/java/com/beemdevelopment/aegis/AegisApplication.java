package com.beemdevelopment.aegis;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.beemdevelopment.aegis.db.DatabaseManager;
import com.beemdevelopment.aegis.ui.MainActivity;

import java.util.Collections;

import androidx.annotation.RequiresApi;

public class AegisApplication extends Application {
    private DatabaseManager _manager;
    private Preferences _prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        _manager = new DatabaseManager(this);
        _prefs = new Preferences(this);

        // listen for SCREEN_OFF events
        ScreenOffReceiver receiver = new ScreenOffReceiver();
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return _manager;
    }

    public Preferences getPreferences() {
        return _prefs;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void initAppShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("action", "scan");
        intent.setAction(Intent.ACTION_MAIN);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "shortcut_new")
                .setShortLabel(getString(R.string.new_profile))
                .setLongLabel(getString(R.string.add_new_profile))
                .setIcon(Icon.createWithResource(this, R.drawable.qr_scanner))
                .setIntent(intent)
                .build();

        shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
    }

    public boolean isAutoLockEnabled() {
        return _prefs.isAutoLockEnabled() && _manager.isEncryptionEnabled() && !_manager.isLocked();
    }

    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAutoLockEnabled()) {
                _manager.lock();
                Intent newIntent = new Intent(getApplicationContext(), MainActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(newIntent);
            }
        }
    }
}
