package com.beemdevelopment.aegis; //J:因為是在com.beemdevelopment.aegis資料夾底下(所以package name是這樣)

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

//J:https://developer.android.com/reference/android/app/Application  (Application lib)

public class AegisApplication extends Application { //J:繼承android.app.Application的class Application
    private VaultFile _vaultFile;   //J:vault金庫
    private VaultManager _manager;  //J:金庫管理
    private Preferences _prefs;     //J:偏好
    private List<LockListener> _lockListeners;  //J:讀取動作的listner
    private boolean _blockAutoLock;
    private IconPackManager _iconPackManager;

    private static final String CODE_LOCK_STATUS_ID = "lock_status_channel";//g:test
    private static final String CODE_LOCK_VAULT_ACTION = "lock_vault";

    static {
        // to access other app's internal storage directory, run libsu commands inside the global mount namespace
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }


    //Called when the application is starting, before any activity, service,
    // or receiver objects (excluding content providers) have been created.
    // J:Application在start前呼叫的函數
    @Override
    public void onCreate() {
        super.onCreate();   //J:super存取父類別的函數(因為前面override，如果沒有super，則都要重寫)
        _prefs = new Preferences(this); // J:Constructure 建立 _prefs 物件
        _lockListeners = new ArrayList<>();
        _iconPackManager = new IconPackManager(this);

        Iconics.init(this);
        Iconics.registerFont(new MaterialDesignIconic());

        // listen for SCREEN_OFF events
        // J:當螢幕關閉時的event
        ScreenOffReceiver receiver = new ScreenOffReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(CODE_LOCK_VAULT_ACTION);
        registerReceiver(receiver, intentFilter);

        // lock the app if the user moves the application to the background
        // J:若使用者將app移動到背景執行，則lock住app
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());

        // clear the cache directory on startup, to make sure no temporary vault export files remain
        // J:清理cache
        IOUtils.clearDirectory(getCacheDir(), false);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initAppShortcuts();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannels();
        }
    }
    /* J:判斷金庫是否被Lock住，若 _manager為空，返回true
    _manager不為空，返回false */
    public boolean isVaultLocked() {
        return _manager == null;
    }

    /**
     * Loads the vault file from disk at the default location, stores an internal
     * reference to it for future use and returns it. This must only be called before
     * initVaultManager() or after lock().
     */
    // J:這段應該是匯入vault檔案
    // J:有一個class VaultFile，裡面應該是匯入檔案的一些設定
    public VaultFile loadVaultFile() throws VaultManagerException {
        // _manager不為空(返回false，執行例外exception)
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
    // J:初始化VaultManager 物件 _manager，並返回 _manager
    public VaultManager initVaultManager(Vault vault, VaultFileCredentials creds) {
        _vaultFile = null;
        _manager = new VaultManager(this, vault, creds);
        return _manager;
    }

    // J:回傳 _manager(VaultManager物件)
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
    // J:@RequreApi加入僅只是讓編譯成功(但並沒有避免到低版本去運行高版本api的問題，也就是還要針對低版本使用不同的api)
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
    // J:import androidx.lifecycle
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
    // J:import android.content.BroadcastReceiver
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
