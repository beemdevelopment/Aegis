package com.beemdevelopment.aegis.vault;

import android.app.backup.BackupManager;
import android.content.Context;

import androidx.core.util.AtomicFile;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.util.IOUtils;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;

public class VaultManager {
    public static final String FILENAME = "aegis.json";
    public static final String FILENAME_PREFIX_EXPORT = "aegis-export";
    public static final String FILENAME_PREFIX_EXPORT_PLAIN = "aegis-export-plain";
    public static final String FILENAME_PREFIX_EXPORT_URI = "aegis-export-uri";

    private Vault _vault;
    private VaultFileCredentials _creds;

    private Context _context;
    private Preferences _prefs;
    private VaultBackupManager _backups;
    private BackupManager _androidBackups;

    public VaultManager(Context context, Vault vault, VaultFileCredentials creds) {
        _context = context;
        _prefs = new Preferences(context);
        _backups = new VaultBackupManager(context);
        _androidBackups = new BackupManager(context);
        _vault = vault;
        _creds = creds;
    }

    public VaultManager(Context context, Vault vault) {
        this(context, vault, null);
    }

    public static AtomicFile getAtomicFile(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), FILENAME));
    }

    public static boolean fileExists(Context context) {
        File file = getAtomicFile(context).getBaseFile();
        return file.exists() && file.isFile();
    }

    public static void deleteFile(Context context) {
        getAtomicFile(context).delete();
    }

    public static VaultFile readVaultFile(Context context) throws VaultManagerException {
        AtomicFile file = getAtomicFile(context);

        try {
            byte[] fileBytes = file.readFully();
            return VaultFile.fromBytes(fileBytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultManagerException(e);
        }
    }

    public static void writeToFile(Context context, InputStream inStream) throws IOException {
        AtomicFile file = VaultManager.getAtomicFile(context);

        FileOutputStream outStream = null;
        try {
            outStream = file.startWrite();
            IOUtils.copy(inStream, outStream);
            file.finishWrite(outStream);
        } catch (IOException e) {
            if (outStream != null) {
                file.failWrite(outStream);
            }
            throw e;
        }
    }

    public static VaultManager init(Context context, VaultFile file, VaultFileCredentials creds) throws VaultManagerException {
        if (file.isEncrypted() && creds == null) {
            throw new IllegalArgumentException("The VaultFile is encrypted but the given VaultFileCredentials is null");
        }

        Vault vault;
        try {
            JSONObject obj;
            if (!file.isEncrypted()) {
                obj = file.getContent();
            } else {
                obj = file.getContent(creds);
            }

            vault = Vault.fromJson(obj);
        } catch (VaultException | VaultFileException e) {
            throw new VaultManagerException(e);
        }

        return new VaultManager(context, vault, creds);
    }

    public static void save(Context context, VaultFile vaultFile) throws VaultManagerException {
        try {
            byte[] bytes = vaultFile.toBytes();
            writeToFile(context, new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new VaultManagerException(e);
        }
    }

    public void destroy() {
        _backups.destroy();
    }

    public void save(boolean backup) throws VaultManagerException {
        try {
            JSONObject obj = _vault.toJson();

            VaultFile file = new VaultFile();
            if (isEncryptionEnabled()) {
                file.setContent(obj, _creds);
            } else {
                file.setContent(obj);
            }

            save(_context, file);
        } catch (VaultFileException e) {
            throw new VaultManagerException(e);
        }

        if (backup) {
            if (_prefs.isBackupsEnabled()) {
                try {
                    backup();
                    _prefs.setBackupsError(null);
                } catch (VaultManagerException e) {
                    _prefs.setBackupsError(e);
                }
            }

            if (_prefs.isAndroidBackupsEnabled()) {
                androidBackupDataChanged();
            }
        }
    }

    /**
     * Exports the vault bt serializing it and writing it to the given OutputStream. If encryption
     * is enabled, the vault will be encrypted automatically.
     */
    public void export(OutputStream stream) throws VaultManagerException {
        export(stream, getCredentials());
    }

    /**
     * Exports the vault by serializing it and writing it to the given OutputStream. If creds is
     * not null, it will be used to encrypt the vault first.
     */
    public void export(OutputStream stream, VaultFileCredentials creds) throws VaultManagerException {
        try {
            VaultFile vaultFile = new VaultFile();
            if (creds != null) {
                vaultFile.setContent(_vault.toJson(), creds);
            } else {
                vaultFile.setContent(_vault.toJson());
            }

            byte[] bytes = vaultFile.toBytes();
            stream.write(bytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultManagerException(e);
        }
    }

    /**
     * Exports the vault by serializing the list of entries to a newline-separated list of
     * Google Authenticator URI's and writing it to the given OutputStream.
     */
    public void exportGoogleUris(OutputStream outStream) throws VaultManagerException {
        try (PrintStream stream = new PrintStream(outStream, false, StandardCharsets.UTF_8.name())) {
            for (VaultEntry entry : getEntries()) {
                GoogleAuthInfo info = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                stream.println(info.getUri().toString());
            }
        } catch (IOException e) {
            throw new VaultManagerException(e);
        }
    }

    public void backup() throws VaultManagerException {
        try {
            File dir = new File(_context.getCacheDir(), "backup");
            if (!dir.exists() && !dir.mkdir()) {
                throw new IOException(String.format("Unable to create directory %s", dir));
            }

            File tempFile = File.createTempFile(VaultBackupManager.FILENAME_PREFIX, ".json", dir);
            try (InputStream inStream = getAtomicFile(_context).openRead();
                OutputStream outStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(inStream, outStream);
            }

            _backups.scheduleBackup(tempFile, _prefs.getBackupsLocation(), _prefs.getBackupsVersionCount());
        } catch (IOException e) {
            throw new VaultManagerException(e);
        }
    }

    public void androidBackupDataChanged() {
        _androidBackups.dataChanged();
    }

    public void addEntry(VaultEntry entry) {
        _vault.getEntries().add(entry);
    }

    public VaultEntry getEntryByUUID(UUID uuid) {
        return _vault.getEntries().getByUUID(uuid);
    }

    public VaultEntry removeEntry(VaultEntry entry) {
        return _vault.getEntries().remove(entry);
    }

    public void wipeEntries() {
        _vault.getEntries().wipe();
    }

    public VaultEntry replaceEntry(VaultEntry entry) {
        return _vault.getEntries().replace(entry);
    }

    public void swapEntries(VaultEntry entry1, VaultEntry entry2) {
        _vault.getEntries().swap(entry1, entry2);
    }

    public boolean isEntryDuplicate(VaultEntry entry) {
        return _vault.getEntries().has(entry);
    }

    public Collection<VaultEntry> getEntries() {
        return _vault.getEntries().getValues();
    }

    public TreeSet<String> getGroups() {
        TreeSet<String> groups = new TreeSet<>(Collator.getInstance());
        for (VaultEntry entry : getEntries()) {
            groups.add(entry.getGroup());
        }
        return groups;
    }

    public VaultFileCredentials getCredentials() {
        return _creds;
    }

    public void setCredentials(VaultFileCredentials creds) {
        _creds = creds;
    }

    public boolean isEncryptionEnabled() {
        return _creds != null;
    }

    public void enableEncryption(VaultFileCredentials creds) throws VaultManagerException {
        _creds = creds;
        save(true);
    }

    public void disableEncryption() throws VaultManagerException {
        _creds = null;
        save(true);
    }
}
