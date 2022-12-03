package com.beemdevelopment.aegis.vault;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class VaultManager {
    private final Context _context;
    private final Preferences _prefs;

    private VaultFile _vaultFile;
    private VaultRepositoryException _vaultFileError;
    private VaultRepository _repo;

    private final VaultBackupManager _backups;
    private final BackupManager _androidBackups;

    private final List<LockListener> _lockListeners;
    private boolean _blockAutoLock;

    public VaultManager(@NonNull Context context) {
        _context = context;
        _prefs = new Preferences(_context);
        _backups = new VaultBackupManager(_context);
        _androidBackups = new BackupManager(context);
        _lockListeners = new ArrayList<>();
        loadVaultFile();
    }

    private void loadVaultFile() {
        try {
            _vaultFile = VaultRepository.readVaultFile(_context);
        } catch (VaultRepositoryException e) {
            if (!(e.getCause() instanceof FileNotFoundException)) {
                _vaultFileError = e;
                e.printStackTrace();
            }
        }

        if (_vaultFile != null && !_vaultFile.isEncrypted()) {
            try {
                loadFrom(_vaultFile, null);
            } catch (VaultRepositoryException e) {
                e.printStackTrace();
                _vaultFile = null;
                _vaultFileError = e;
            }
        }
    }

    /**
     * Initializes the vault repository with a new empty vault and the given creds. It can
     * only be called if isVaultLoaded() returns false.
     *
     * Calling this method removes the manager's internal reference to the raw vault file (if it had one).
     */
    @NonNull
    public VaultRepository initNew(@Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        if (isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is already initialized");
        }

        _vaultFile = null;
        _vaultFileError = null;
        _repo = new VaultRepository(_context, new Vault(), creds);
        save();

        if (getVault().isEncryptionEnabled()) {
            startNotificationService();
        }

        return getVault();
    }

    /**
     * Initializes the vault repository by decrypting the given vaultFile with the given
     * creds. It can only be called if isVaultLoaded() returns false.
     *
     * Calling this method removes the manager's internal reference to the raw vault file (if it had one).
     */
    @NonNull
    public VaultRepository loadFrom(@NonNull VaultFile vaultFile, @Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        if (isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is already initialized");
        }

        _vaultFile = null;
        _vaultFileError = null;
        _repo = VaultRepository.fromFile(_context, vaultFile, creds);

        if (getVault().isEncryptionEnabled()) {
            startNotificationService();
        }

        return getVault();
    }

    /**
     * Initializes the vault repository by loading and decrypting the vault file stored in
     * internal storage, with the given creds. It can only be called if isVaultLoaded()
     * returns false.
     *
     * Calling this method removes the manager's internal reference to the raw vault file (if it had one).
     */
    @NonNull
    public VaultRepository load(@Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        if (isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is already initialized");
        }

        loadVaultFile();
        if (isVaultLoaded()) {
            return _repo;
        }

        return loadFrom(getVaultFile(), creds);
    }

    @NonNull
    public VaultRepository unlock(@NonNull VaultFileCredentials creds) throws VaultRepositoryException {
        VaultRepository repo = loadFrom(getVaultFile(), creds);
        startNotificationService();
        return repo;
    }

    /**
     * Locks the vault and the app.
     * @param userInitiated whether or not the user initiated the lock in MainActivity.
     */
    public void lock(boolean userInitiated) {
        _repo = null;

        for (LockListener listener : _lockListeners) {
            listener.onLocked(userInitiated);
        }

        stopNotificationService();
        loadVaultFile();
    }

    public void enableEncryption(VaultFileCredentials creds) throws VaultRepositoryException {
        getVault().setCredentials(creds);
        saveAndBackup();
        startNotificationService();
    }

    public void disableEncryption() throws VaultRepositoryException {
        getVault().setCredentials(null);
        save();

        // remove any keys that are stored in the KeyStore
        try {
            KeyStoreHandle handle = new KeyStoreHandle();
            handle.clear();
        } catch (KeyStoreHandleException e) {
            // this cleanup operation is not strictly necessary, so we ignore any exceptions here
            e.printStackTrace();
        }

        stopNotificationService();
    }

    public void save() throws VaultRepositoryException {
        getVault().save();
    }

    public void saveAndBackup() throws VaultRepositoryException {
        save();

        boolean backedUp = false;
        if (getVault().isEncryptionEnabled()) {
            if (_prefs.isBackupsEnabled()) {
                backedUp = true;
                try {
                    scheduleBackup();
                } catch (VaultRepositoryException e) {
                    _prefs.setBuiltInBackupResult(new Preferences.BackupResult(e));
                }
            }

            if (_prefs.isAndroidBackupsEnabled()) {
                backedUp = true;
                scheduleAndroidBackup();
            }
        }

        if (!backedUp) {
            _prefs.setIsBackupReminderNeeded(true);
        }
    }

    public void scheduleBackup() throws VaultRepositoryException {
        _prefs.setIsBackupReminderNeeded(false);

        try {
            File dir = new File(_context.getCacheDir(), "backup");
            if (!dir.exists() && !dir.mkdir()) {
                throw new IOException(String.format("Unable to create directory %s", dir));
            }

            File tempFile = File.createTempFile(VaultBackupManager.FILENAME_PREFIX, ".json", dir);
            try (OutputStream outStream = new FileOutputStream(tempFile)) {
                _repo.export(outStream);
            }

            _backups.scheduleBackup(tempFile, _prefs.getBackupsLocation(), _prefs.getBackupsVersionCount());
        } catch (IOException e) {
            throw new VaultRepositoryException(e);
        }
    }

    public void scheduleAndroidBackup() {
        _prefs.setIsBackupReminderNeeded(false);
        _androidBackups.dataChanged();
    }

    public boolean isAutoLockEnabled(int autoLockType) {
        return _prefs.isAutoLockTypeEnabled(autoLockType)
                && isVaultLoaded()
                && getVault().isEncryptionEnabled();
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
     * Reports whether automatic lock on minimization is currently blocked.
     */
    public boolean isAutoLockBlocked() {
        return _blockAutoLock;
    }

    public boolean isVaultLoaded() {
        return _repo != null;
    }

    public boolean isVaultFileLoaded() {
        return _vaultFile != null;
    }

    public boolean isVaultInitNeeded() {
        return !isVaultLoaded() && !isVaultFileLoaded() && getVaultFileError() == null;
    }

    @NonNull
    public VaultRepository getVault() {
        if (!isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is not initialized");
        }

        return _repo;
    }

    @NonNull
    public VaultFile getVaultFile() {
        if (_vaultFile == null) {
            throw new IllegalStateException("Vault file is not in memory");
        }

        return _vaultFile;
    }

    @Nullable
    public VaultRepositoryException getVaultFileError() {
        return _vaultFileError;
    }

    /**
     * Starts an external activity, temporarily blocks automatic lock of Aegis and
     * shows an error dialog if the target activity is not found.
     */
    public void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        setBlockAutoLock(true);

        try {
            activity.startActivityForResult(intent, requestCode, null);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(activity, R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Starts an external activity, temporarily blocks automatic lock of Aegis and
     * shows an error dialog if the target activity is not found.
     */
    public void startActivity(Fragment fragment, Intent intent) {
        startActivityForResult(fragment, intent, -1);
    }

    /**
     * Starts an external activity, temporarily blocks automatic lock of Aegis and
     * shows an error dialog if the target activity is not found.
     */
    public void startActivityForResult(Fragment fragment, Intent intent, int requestCode) {
        setBlockAutoLock(true);

        try {
            fragment.startActivityForResult(intent, requestCode, null);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(fragment.requireContext(), R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    private void startNotificationService() {
        // NOTE: Disabled for now. See issue: #1047
        /*if (PermissionHelper.granted(_context, Manifest.permission.POST_NOTIFICATIONS)) {
            _context.startService(getNotificationServiceIntent());
        }*/
    }

    private void stopNotificationService() {
        // NOTE: Disabled for now. See issue: #1047
        //_context.stopService(getNotificationServiceIntent());
    }

    private Intent getNotificationServiceIntent() {
        return new Intent(_context, NotificationService.class);
    }

    private static boolean isDocsAction(@Nullable String action) {
        return action != null && (action.equals(Intent.ACTION_GET_CONTENT)
                || action.equals(Intent.ACTION_CREATE_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }

    public interface LockListener {
        /**
         * Called when the vault lock status changes
         * @param userInitiated whether or not the user initiated the lock in MainActivity.
         */
        void onLocked(boolean userInitiated);
    }
}
