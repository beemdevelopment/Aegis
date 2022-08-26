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

import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Iterator;

import dagger.hilt.InstallIn;
import dagger.hilt.android.EarlyEntryPoint;
import dagger.hilt.android.EarlyEntryPoints;
import dagger.hilt.components.SingletonComponent;

public abstract class AegisApplicationBase extends Application {
    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";
    private static final String CODE_LOCK_VAULT_ACTION = "lock_vault";

    private VaultManager _vaultManager;

    static {
        // to access other app's internal storage directory, run libsu commands inside the global mount namespace
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _vaultManager = EarlyEntryPoints.get(this, EntryPoint.class).getVaultManager();

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void initAppShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            return;
        }
        try {
            int maxShortcuts = shortcutManager.getMaxShortcutCountPerActivity();
            if (maxShortcuts > 0) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("action", "scan");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.setAction(Intent.ACTION_MAIN);
                ArrayList<ShortcutInfo> shortcuts = new ArrayList<>(maxShortcuts);
                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "shortcut_new")
                        .setShortLabel(getString(R.string.new_entry))
                        .setLongLabel(getString(R.string.add_new_entry))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_qr_code))
                        .setIntent(intent)
                        .setRank(0)
                        .build();
                shortcuts.add(shortcut);

                // TODO order by most recently used
                if(_vaultManager.isVaultLoaded()) {
                    final Iterator<String> groups = _vaultManager.getVault().getGroups().iterator();
                    while (shortcuts.size() < maxShortcuts && groups.hasNext()) {
                        addShortcutForGroup(shortcuts, groups.next());
                    }
                }
                shortcutManager.setDynamicShortcuts(shortcuts);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void addShortcutForGroup(ArrayList<ShortcutInfo> shortcuts, String group) {
        final Intent groupFilterIntent = new Intent(this, MainActivity.class);
        groupFilterIntent.putExtra("action", "set_group_filter");
        groupFilterIntent.putExtra("group", group);
        groupFilterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        groupFilterIntent.setAction(Intent.ACTION_MAIN);
        String shortLabel = group;
        if (shortLabel.length() > 10) {
            shortLabel = shortLabel.substring(0, 9) + "…";
        }
        final String longLabel = group; // TODO maybe "Open group %1$s"?
        shortcuts.add(
                new ShortcutInfo.Builder(this, "shortcut_group_" + group)
                        .setShortLabel(shortLabel)
                        .setLongLabel(longLabel)
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(groupFilterIntent)
                        .setRank(1)
                        .build()
        );
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
                    && _vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_MINIMIZE)
                    && !_vaultManager.isAutoLockBlocked()) {
                _vaultManager.lock(false);
            }
        }
    }

    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_DEVICE_LOCK)) {
                _vaultManager.lock(false);
            }
        }
    }

    @EarlyEntryPoint
    @InstallIn(SingletonComponent.class)
    interface EntryPoint {
        VaultManager getVaultManager();
    }
}
