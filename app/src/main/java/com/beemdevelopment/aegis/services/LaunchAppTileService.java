package com.beemdevelopment.aegis.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.beemdevelopment.aegis.ui.MainActivity;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LaunchAppTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        // Centralized logic for Direct Boot safety
        if (isDeviceLocked()) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
            return;
        }

        try {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        } catch (Exception e) {
            Log.e("LaunchAppTileService", "QS Tile refresh failed", e);
        }
    }

    private boolean isDeviceLocked() {
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        return um != null && !um.isUserUnlocked();
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();
        // Clicking is only possible if the OS has enabled the tile
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(Intent.ACTION_MAIN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
