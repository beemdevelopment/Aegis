package me.impy.aegis;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import java.util.Collections;

import me.impy.aegis.db.DatabaseManager;

public class AegisApplication extends Application {
    private boolean _running = false;
    private DatabaseManager _manager = new DatabaseManager(this);

    @Override
    public void onCreate() {
        super.onCreate();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return _manager;
    }

    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    public boolean isRunning() {
        // return false the first time this is called
        if (_running) {
            return true;
        }
        _running = true;
        return false;
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
                .setShortLabel("New profile")
                .setLongLabel("Add new profile")
                .setIcon(Icon.createWithResource(this, R.drawable.intro_scanner))
                .setIntent(intent)
                .build();

        shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
    }
}
