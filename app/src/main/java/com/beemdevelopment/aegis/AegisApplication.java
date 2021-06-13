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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.beemdevelopment.aegis.icons.IconPackManager;
import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AegisApplication extends Application {
    private VaultFile _vaultFile;
    private VaultManager _manager;
    private Preferences _prefs;
    private List<LockListener> _lockListeners;
    private boolean _blockAutoLock;
    private IconPackManager _iconPackManager;

    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";
    private static final String CODE_LOCK_VAULT_ACTION = "lock_vault";

    static {
        // to access other app's internal storage directory, run libsu commands inside the global mount namespace
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _prefs = new Preferences(this);
        _lockListeners = new ArrayList<>();
        _iconPackManager = new IconPackManager(this);

        Iconics.init(this);
        Iconics.registerFont(new MaterialDesignIconic());

        // listen for SCREEN_OFF events
        ScreenOffReceiver receiver = new ScreenOffReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(CODE_LOCK_VAULT_ACTION);
        registerReceiver(receiver, intentFilter);

        // lock the app if the user moves the application to the background
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());

        // clear the cache directory on startup, to make sure no temporary vault export files remain
        IOUtils.clearDirectory(getCacheDir(), false);

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
            _vaultFile = VaultManager.readVaultFile(this);
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

    public IconPackManager getIconPackManager() {
        return _iconPackManager;
    }

    public Preferences getPreferences() {
        return _prefs;
    }

    public boolean isAutoLockEnabled(int autoLockType) {
        return _prefs.isAutoLockTypeEnabled(autoLockType) && !isVaultLocked() && _manager.isEncryptionEnabled();
    }

    public void registerLockListener(LockListener listener) {
        _lockListeners.add(listener);
    }

    public void unregisterLockListener(LockListener listener) {
        _lockListeners.remove(listener);
    }

    /**
     * Sets whether to block automatic lock on minimization. This should only be called
     * by activities before invoking an intent that shows a DocumentsUI, because that
     * action leads AppLifecycleObserver to believe that the app has been minimized.
     */
    public void setBlockAutoLock(boolean block) {
        _blockAutoLock = block;
    }

    /**
     * Locks the vault and the app.
     * @param userInitiated whether or not the user initiated the lock in MainActivity.
     */
    public void lock(boolean userInitiated) {
        _manager.destroy();
        _manager = null;

        for (LockListener listener : _lockListeners) {
            listener.onLocked(userInitiated);
        }

        stopService(new Intent(AegisApplication.this, NotificationService.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void initAppShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            return;
        }

        List<ShortcutInfo> shortcuts = new ArrayList<>();
        Intent newIntent = new Intent(this, MainActivity.class);
        newIntent.putExtra("action", "scan");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        newIntent.setAction(Intent.ACTION_MAIN);

        shortcuts.add(
            new ShortcutInfo.Builder(this, "shortcut_new")
                .setShortLabel(getString(R.string.new_entry))
                .setLongLabel(getString(R.string.add_new_entry))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_qr_code))
                .setIntent(newIntent)
                .setRank(0)
                .build()
        );


        Intent groupFilterIntent = new Intent(this, MainActivity.class);
        groupFilterIntent.putExtra("action", "set_group_filter");

        List<String> groups = getPreferences().getGroupFilter();
        if(!groups.isEmpty()){
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < groups.size(); i++) {
                builder.append(groups.get(i));
                if(i != groups.size() - 1)
                    builder.append(", ");
            }

            String shortLabel = builder.toString();
            if(shortLabel.length() > 10)
                shortLabel = shortLabel.substring(0, 7) + "...";
            String longLabel = getResources().getQuantityString(R.plurals.open_groups, groups.size(), builder.toString());

            groupFilterIntent.putExtra("groups", groups.toArray(new String[0]));
            groupFilterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            groupFilterIntent.setAction(Intent.ACTION_MAIN);
            shortcuts.add(
                 new ShortcutInfo.Builder(this, UUID.randomUUID().toString())
                    .setShortLabel(shortLabel)
                    .setLongLabel(longLabel)
                    .setIcon(Icon.createWithResource(this, R.drawable.app_icon))
                    .setIntent(groupFilterIntent)
                    .setRank(1)
                    .build()
            );

        }
        shortcutManager.setDynamicShortcuts(shortcuts);
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

    private class AppLifecycleObserver implements LifecycleEventObserver {
        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_STOP
                    && isAutoLockEnabled(Preferences.AUTO_LOCK_ON_MINIMIZE)
                    && !_blockAutoLock) {
                lock(false);
            }
        }
    }

    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAutoLockEnabled(Preferences.AUTO_LOCK_ON_DEVICE_LOCK)) {
                lock(false);
            }
        }
    }

    public interface LockListener {
        /**
         * When called, the app/vault has been locked and the listener should perform its cleanup operations.
         * @param userInitiated whether or not the user initiated the lock in MainActivity.
         */
        void onLocked(boolean userInitiated);
    }
}
