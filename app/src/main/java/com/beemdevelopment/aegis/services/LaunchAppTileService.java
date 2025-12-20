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
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        // Check if the credential-encrypted storage is unlocked
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (userManager != null && !userManager.isUserUnlocked()) {
            // Keep tile in safe inactive state until the user unlocks the phone
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
            return;
        }

        try {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        } catch (Exception e) {
            Log.e("LaunchAppTileService", "Failed to update tile state", e);
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

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
