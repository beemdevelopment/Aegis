package com.beemdevelopment.aegis;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AegisApplication extends Application {
    private VaultFile _vaultFile;
    private VaultManager _manager;
    private Preferences _prefs;
    private List<LockListener> _lockListeners;

    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";
    private static final String CODE_LOCK_VAULT_ACTION = "lock_vault";

    @Override
    public void onCreate() {
        super.onCreate();
        _prefs = new Preferences(this);
        _lockListeners = new ArrayList<>();

        Iconics.init(this);
        Iconics.registerFont(new MaterialDesignIconic());

        // listen for SCREEN_OFF events
        ScreenOffReceiver receiver = new ScreenOffReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(CODE_LOCK_VAULT_ACTION);
        registerReceiver(receiver, intentFilter);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannels();
        }
    }

    public boolean isVaultLocked() {
        return _manager == null;
    }

    /**
     * Loads the vault file from disk at the default location, stores an internal
     * reference to it for future use and returns it. This must only be called before
     * initVaultManager() or after lock().
     */
    public VaultFile loadVaultFile() throws VaultManagerException {
        if (!isVaultLocked()) {
            throw new AssertionError("loadVaultFile() may only be called before initVaultManager() or after lock()");
        }

        if (_vaultFile == null) {
            _vaultFile = VaultManager.readFile(this);
        }

        return _vaultFile;
    }

    /**
     * Initializes the vault manager by decrypting the given vaultFile with the given
     * creds. This removes the internal reference to the raw vault file.
     */
    public VaultManager initVaultManager(VaultFile vaultFile, VaultFileCredentials creds) throws VaultManagerException {
        _vaultFile = null;
        _manager = VaultManager.init(this, vaultFile, creds);
        return _manager;
    }

    /**
     * Initializes the vault manager with the given vault and creds. This removes the
     * internal reference to the raw vault file.
     */
    public VaultManager initVaultManager(Vault vault, VaultFileCredentials creds) {
        _vaultFile = null;
        _manager = new VaultManager(this, vault, creds);
        return _manager;
    }

    public VaultManager getVaultManager() {
        return _manager;
    }

    public Preferences getPreferences() {
        return _prefs;
    }

    public boolean isAutoLockEnabled() {
        return _prefs.isAutoLockEnabled() && !isVaultLocked() && _manager.isEncryptionEnabled() ;
    }

    public void registerLockListener(LockListener listener) {
        _lockListeners.add(listener);
    }

    public void unregisterLockListener(LockListener listener) {
        _lockListeners.remove(listener);
    }

    public void lock() {
        _manager = null;
        for (LockListener listener : _lockListeners) {
            listener.onLocked();
        }

        stopService(new Intent(AegisApplication.this, NotificationService.class));
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
                .setShortLabel(getString(R.string.new_entry))
                .setLongLabel(getString(R.string.add_new_entry))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_qr_code))
                .setIntent(intent)
                .build();

        shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
    }

    private void initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name_lock_status);
            String description = getString(R.string.channel_description_lock_status);
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(CODE_LOCK_STATUS_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAutoLockEnabled()) {
                lock();
            }
        }
    }

    public interface LockListener {
        void onLocked();
    }
}
