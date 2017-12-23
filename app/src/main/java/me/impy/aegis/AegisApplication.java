package me.impy.aegis;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.impy.aegis.db.DatabaseManager;

public class AegisApplication extends Application {
    private boolean _running = false;
    private DatabaseManager _manager = new DatabaseManager(this);

    @Override
    public void onCreate() {
        super.onCreate();
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
}
