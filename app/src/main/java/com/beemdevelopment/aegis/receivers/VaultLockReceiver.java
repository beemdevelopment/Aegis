package com.beemdevelopment.aegis.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.services.LaunchAppTileService;
import com.beemdevelopment.aegis.services.LaunchScannerTileService;
import com.beemdevelopment.aegis.vault.VaultManager;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VaultLockReceiver extends BroadcastReceiver {
    public static final String ACTION_LOCK_VAULT = String.format("%s.LOCK_VAULT", BuildConfig.APPLICATION_ID);

    @Inject
    protected VaultManager _vaultManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // Group 1: System-level refresh signals
        if (Intent.ACTION_USER_UNLOCKED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            refreshQuickSettingsTiles(context);
            return; // No need to process vault locking for these signals
        }

        // Group 2: Security/Locking signals
        if (action.equals(ACTION_LOCK_VAULT) || action.equals(Intent.ACTION_SCREEN_OFF)) {
            handleVaultAutoLock();
        }
    }

    private void refreshQuickSettingsTiles(Context context) {
        // Force refresh all Aegis-related tiles
        TileService.requestListeningState(context, new ComponentName(context, LaunchAppTileService.class));
        try {
            TileService.requestListeningState(context, new ComponentName(context, LaunchScannerTileService.class));
        } catch (Exception ignored) {}
    }

    private void handleVaultAutoLock() {
        if (_vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_DEVICE_LOCK)) {
            _vaultManager.lock(false);
        }
    }
}
