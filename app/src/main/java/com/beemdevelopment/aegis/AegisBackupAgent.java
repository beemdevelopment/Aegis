package com.beemdevelopment.aegis;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.beemdevelopment.aegis.database.AppDatabase;
import com.beemdevelopment.aegis.database.AuditLogRepository;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AegisBackupAgent extends BackupAgent {
    private static final String TAG = AegisBackupAgent.class.getSimpleName();

    private Preferences _prefs;

    private AuditLogRepository _auditLogRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        // Cannot use injection with Dagger Hilt here, because the app is launched in a restricted mode on restore
        _prefs = new Preferences(this);
        AppDatabase appDatabase = AegisModule.provideAppDatabase(this);
        _auditLogRepository = AegisModule.provideAuditLogRepository(appDatabase);
    }

    @Override
    public synchronized void onFullBackup(FullBackupDataOutput data) throws IOException {
        Log.i(TAG, String.format("onFullBackup() called: flags=%d, quota=%d",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? data.getTransportFlags() : -1,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? data.getQuota() : -1));

        boolean isD2D = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && (data.getTransportFlags() & FLAG_DEVICE_TO_DEVICE_TRANSFER) == FLAG_DEVICE_TO_DEVICE_TRANSFER;

        if (isD2D) {
            Log.i(TAG, "onFullBackup(): allowing D2D transfer");
        } else if (!_prefs.isAndroidBackupsEnabled()) {
            Log.i(TAG, "onFullBackup() skipped: Android backups disabled in preferences");
            return;
        }

        // We perform a catch of any Exception here to make sure we also
        // report any runtime exceptions, in addition to the expected IOExceptions.
        try {
            fullBackup(data);
            _auditLogRepository.addAndroidBackupCreatedEvent();
            _prefs.setAndroidBackupResult(new Preferences.BackupResult(null));
        } catch (Exception e) {
            Log.e(TAG, String.format("onFullBackup() failed: %s", e));
            _prefs.setAndroidBackupResult(new Preferences.BackupResult(e));
            throw e;
        }

        Log.i(TAG, "onFullBackup() finished");
    }

    private void fullBackup(FullBackupDataOutput data) throws IOException {
        // First copy the vault to the files/backup directory
        createBackupDir();
        File vaultBackupFile = getVaultBackupFile();
        try (OutputStream outputStream = new FileOutputStream(vaultBackupFile)) {
            VaultFile vaultFile = VaultRepository.readVaultFile(this);
            byte[] bytes = vaultFile.exportable().toBytes();
            outputStream.write(bytes);
        } catch (VaultRepositoryException | IOException e) {
            deleteBackupDir();
            throw new IOException(e);
        }

        // Then call the original implementation so that fullBackupContent specified in AndroidManifest is read
        try {
            super.onFullBackup(data);
        } finally {
            deleteBackupDir();
        }
    }

    @Override
    public synchronized void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime) throws IOException {
        Log.i(TAG, String.format("onRestoreFile() called: dest=%s", destination));
        super.onRestoreFile(data, size, destination, type, mode, mtime);

        File vaultBackupFile = getVaultBackupFile();
        if (destination.getCanonicalFile().equals(vaultBackupFile.getCanonicalFile())) {
            try (InputStream inStream = new FileInputStream(vaultBackupFile)) {
                VaultRepository.writeToFile(this, inStream);
            } catch (IOException e) {
                Log.e(TAG, String.format("onRestoreFile() failed: dest=%s, error=%s", destination, e));
                throw e;
            } finally {
                deleteBackupDir();
            }
        }

        Log.i(TAG, String.format("onRestoreFile() finished: dest=%s", destination));
    }

    @Override
    public synchronized void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
        super.onQuotaExceeded(backupDataBytes, quotaBytes);
        Log.e(TAG, String.format("onQuotaExceeded() called: backupDataBytes=%d, quotaBytes=%d", backupDataBytes, quotaBytes));
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {

    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {

    }

    private void createBackupDir() throws IOException {
        File dir = getVaultBackupFile().getParentFile();
        if (dir == null || (!dir.exists() && !dir.mkdir())) {
            throw new IOException(String.format("Unable to create backup directory: %s", dir));
        }
    }

    private void deleteBackupDir() {
        File dir = getVaultBackupFile().getParentFile();
        if (dir != null) {
            IOUtils.clearDirectory(dir, true);
        }
    }

    private File getVaultBackupFile() {
        return new File(new File(getFilesDir(), "backup"), VaultRepository.FILENAME);
    }
}
