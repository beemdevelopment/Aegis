package com.beemdevelopment.aegis.vault;

import android.content.Context;

import androidx.core.util.AtomicFile;

import com.beemdevelopment.aegis.Preferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Collator;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;

public class VaultManager {
    public static final String FILENAME = "aegis.json";
    public static final String FILENAME_EXPORT = "aegis-export.json";
    public static final String FILENAME_EXPORT_PLAIN = "aegis-export-plain.json";

    private Vault _vault;
    private VaultFileCredentials _creds;

    private Context _context;
    private Preferences _prefs;
    private VaultBackupManager _backups;

    public VaultManager(Context context, Vault vault, VaultFileCredentials creds) {
        _context = context;
        _prefs = new Preferences(context);
        _backups = new VaultBackupManager(context);
        _vault = vault;
        _creds = creds;
    }

    public VaultManager(Context context, Vault vault) {
        this(context, vault, null);
    }

    public static boolean fileExists(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        return file.exists() && file.isFile();
    }

    public static VaultFile readFile(Context context) throws VaultManagerException {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILENAME));

        try {
            byte[] fileBytes = file.readFully();
            return VaultFile.fromBytes(fileBytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultManagerException(e);
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
        byte[] bytes = vaultFile.toBytes();
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILENAME));

        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(bytes);
            file.finishWrite(stream);
        } catch (IOException e) {
            if (stream != null) {
                file.failWrite(stream);
            }
            throw new VaultManagerException(e);
        }
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

        if (backup && _prefs.isBackupsEnabled()) {
            try {
                backup();
                _prefs.setBackupsError(null);
            } catch (VaultManagerException e) {
                _prefs.setBackupsError(e);
            }
        }
    }

    public void export(OutputStream stream, boolean encrypt) throws VaultManagerException {
        try {
            VaultFile vaultFile = new VaultFile();
            if (encrypt && isEncryptionEnabled()) {
                vaultFile.setContent(_vault.toJson(), _creds);
            } else {
                vaultFile.setContent(_vault.toJson());
            }

            byte[] bytes = vaultFile.toBytes();
            stream.write(bytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultManagerException(e);
        }
    }

    public void backup() throws VaultManagerException {
        _backups.create(_prefs.getBackupsLocation(), _prefs.getBackupsVersionCount());
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
            String group = entry.getGroup();
            if (group != null) {
                groups.add(group);
            }
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
