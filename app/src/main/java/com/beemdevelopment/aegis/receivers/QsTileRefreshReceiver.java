package com.beemdevelopment.aegis.receivers;

import static android.content.Intent.ACTION_BOOT_COMPLETED;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import com.beemdevelopment.aegis.services.LaunchAppTileService;
import com.beemdevelopment.aegis.services.LaunchScannerTileService;

public class QsTileRefreshReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null
                || (!intent.getAction().equals(ACTION_BOOT_COMPLETED)
                && !intent.getAction().equals(Intent.ACTION_USER_UNLOCKED))) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context,
                    new ComponentName(context, LaunchAppTileService.class));
            TileService.requestListeningState(context,
                    new ComponentName(context, LaunchScannerTileService.class));
        }
    }
}
