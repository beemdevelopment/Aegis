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
    public static final String ACTION_LOCK_VAULT
            = String.format("%s.LOCK_VAULT", BuildConfig.APPLICATION_ID);

    @Inject
    protected VaultManager _vaultManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        // Trigger Tile refresh when the device is unlocked after reboot
        if (action.equals(Intent.ACTION_USER_UNLOCKED) || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            TileService.requestListeningState(context, 
                new ComponentName(context, LaunchAppTileService.class));
            
            // Optionally refresh the scanner tile if available
            try {
                TileService.requestListeningState(context, 
                    new ComponentName(context, LaunchScannerTileService.class));
            } catch (Exception ignored) {}
            return;
        }

        // Existing vault lock logic
        if (!action.equals(ACTION_LOCK_VAULT) && !action.equals(Intent.ACTION_SCREEN_OFF)) {
            return;
        }

        if (_vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_DEVICE_LOCK)) {
            _vaultManager.lock(false);
        }
    }
}
