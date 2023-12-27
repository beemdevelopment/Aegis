package com.beemdevelopment.aegis.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.receivers.VaultLockReceiver;

public class NotificationService extends Service {
    private static final int NOTIFICATION_VAULT_UNLOCKED = 1;

    private static final String CHANNEL_ID = "lock_status_channel";

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        serviceMethod();
        return Service.START_STICKY;
    }

    @SuppressLint("LaunchActivityFromNotification")
    public void serviceMethod() {
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        Intent intent = new Intent(this, VaultLockReceiver.class);
        intent.setAction(VaultLockReceiver.ACTION_LOCK_VAULT);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_aegis_notification)
                .setContentTitle(getString(R.string.app_name_full))
                .setContentText(getString(R.string.vault_unlocked_state))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        // NOTE: Disabled for now. See issue: #1047
        //startForeground(NOTIFICATION_VAULT_UNLOCKED, builder.build());
    }

    @Override
    public void onDestroy() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_VAULT_UNLOCKED);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
