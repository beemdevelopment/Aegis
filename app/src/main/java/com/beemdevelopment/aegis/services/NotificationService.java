package com.beemdevelopment.aegis.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.R;

public class NotificationService extends Service {
    public static final int DATABASE_UNLOCKED_ID = 1;

    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";
    private static final String CODE_LOCK_DATABASE_ACTION = "lock_database";

    public NotificationService() {

    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);

        serviceMethod();
        return Service.START_STICKY;
    }

    public void serviceMethod() {
        Intent intentAction = new Intent(CODE_LOCK_DATABASE_ACTION);
        PendingIntent lockDatabaseIntent = PendingIntent.getBroadcast(this,1,intentAction, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,  CODE_LOCK_STATUS_ID)
                .setSmallIcon(R.drawable.ic_fingerprint_black_24dp)
                .setContentTitle(getString(R.string.app_name_full))
                .setContentText(getString(R.string.vault_unlocked_state))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(lockDatabaseIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(DATABASE_UNLOCKED_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(DATABASE_UNLOCKED_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
