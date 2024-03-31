package com.beemdevelopment.aegis;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.room.Room;

import com.beemdevelopment.aegis.database.AppDatabase;
import com.beemdevelopment.aegis.database.AuditLogEntry;
import com.beemdevelopment.aegis.database.AuditLogRepository;
import com.beemdevelopment.aegis.receivers.VaultLockReceiver;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.topjohnwu.superuser.Shell;

import org.checkerframework.checker.units.qual.A;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.InstallIn;
import dagger.hilt.android.EarlyEntryPoint;
import dagger.hilt.android.EarlyEntryPoints;
import dagger.hilt.components.SingletonComponent;

public abstract class AegisApplicationBase extends Application {
    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";

    private VaultManager _vaultManager;

    static {
        // Enable verbose libsu logging in debug builds
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _vaultManager = EarlyEntryPoints.get(this, EntryPoint.class).getVaultManager();

        VaultLockReceiver lockReceiver = new VaultLockReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        ContextCompat.registerReceiver(this, lockReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // lock the app if the user moves the application to the background
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());

        // clear the cache directory on startup, to make sure no temporary vault export files remain
        IOUtils.clearDirectory(getCacheDir(), false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }

        // NOTE: Disabled for now. See issue: #1047
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannels();
        }*/
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void initAppShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("action", "scan");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

    @EarlyEntryPoint
    @InstallIn(SingletonComponent.class)
    interface EntryPoint {
        VaultManager getVaultManager();
    }
}
