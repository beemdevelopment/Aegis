package com.beemdevelopment.aegis.vault;

import android.content.Context;
import android.content.Intent;

import com.beemdevelopment.aegis.services.NotificationService;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Collator;
import java.util.Collection;
import java.util.TreeSet;

public class VaultManager {
    private static final String FILENAME = "aegis.json";
    public static final String FILENAME_EXPORT = "aegis_export.json";
    public static final String FILENAME_EXPORT_PLAIN = "aegis_export_plain.json";

    private Vault _vault;
    private VaultFile _file;
    private VaultFileCredentials _creds;
    private boolean _encrypt;

    private Context _context;

    public VaultManager(Context context) {
        _context = context;
    }

    public boolean fileExists() {
        File file = new File(_context.getFilesDir(), FILENAME);
        return file.exists() && file.isFile();
    }

    public void load() throws VaultManagerException {
        assertState(true, false);

        try (FileInputStream file = _context.openFileInput(FILENAME)) {
            byte[] fileBytes = new byte[(int) file.getChannel().size()];
            DataInputStream stream = new DataInputStream(file);
            stream.readFully(fileBytes);
            stream.close();

            _file = VaultFile.fromBytes(fileBytes);
            _encrypt = _file.isEncrypted();
            if (!isEncryptionEnabled()) {
                JSONObject obj = _file.getContent();
                _vault = Vault.fromJson(obj);
            }
        } catch (IOException | VaultFileException | VaultException e) {
            throw new VaultManagerException(e);
        }
    }

    public void lock() {
        assertState(false, true);
        _creds = null;
        _vault = null;
    }

    public void unlock(VaultFileCredentials creds) throws VaultManagerException {
        assertState(true, true);

        try {
            JSONObject obj = _file.getContent(creds);
            _vault = Vault.fromJson(obj);
            _creds = creds;
            _context.startService(new Intent(_context, NotificationService.class));
        } catch (VaultFileException | VaultException e) {
            throw new VaultManagerException(e);
        }
    }

    public static void save(Context context, VaultFile file) throws VaultManagerException {
        byte[] bytes = file.toBytes();
        try (FileOutputStream stream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)) {
            stream.write(bytes);
        } catch (IOException e) {
            throw new VaultManagerException(e);
        }
    }

    public void save() throws VaultManagerException {
        assertState(false, true);

        try {
            JSONObject obj = _vault.toJson();
            if (isEncryptionEnabled()) {
                _file.setContent(obj, _creds);
            } else {
                _file.setContent(obj);
            }
            save(_context, _file);
        } catch (VaultFileException e) {
            throw new VaultManagerException(e);
        }
    }

    public void export(OutputStream stream, boolean encrypt) throws VaultManagerException {
        assertState(false, true);

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

    public void addEntry(VaultEntry entry) {
        assertState(false, true);
        _vault.getEntries().add(entry);
    }

    public VaultEntry removeEntry(VaultEntry entry) {
        assertState(false, true);
        return _vault.getEntries().remove(entry);
    }

    public VaultEntry replaceEntry(VaultEntry entry) {
        assertState(false, true);
        return _vault.getEntries().replace(entry);
    }

    public void swapEntries(VaultEntry entry1, VaultEntry entry2) {
        assertState(false, true);
        _vault.getEntries().swap(entry1, entry2);
    }

    public boolean isEntryDuplicate(VaultEntry entry) {
        assertState(false, true);
        return _vault.getEntries().has(entry);
    }

    public Collection<VaultEntry> getEntries() {
        assertState(false, true);
        return _vault.getEntries().getValues();
    }

    public TreeSet<String> getGroups() {
        assertState(false, true);

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
        assertState(false, true);
        return _creds;
    }

    public void setCredentials(VaultFileCredentials creds) {
        assertState(false, true);
        _creds = creds;
    }

    public VaultFile.Header getFileHeader() {
        assertLoaded(true);
        return _file.getHeader();
    }

    public boolean isEncryptionEnabled() {
        assertLoaded(true);
        return _encrypt;
    }

    public void enableEncryption(VaultFileCredentials creds) throws VaultManagerException {
        assertState(false, true);
        _creds = creds;
        _encrypt = true;
        save();
    }

    public void disableEncryption() throws VaultManagerException {
        assertState(false, true);
        _creds = null;
        _encrypt = false;
        save();
    }

    public boolean isLoaded() {
        return _file != null;
    }

    public boolean isLocked() {
        return _vault == null;
    }

    private void assertState(boolean locked, boolean loaded) {
        assertLoaded(loaded);

        if (isLocked() && !locked) {
            throw new AssertionError("vault file has not been unlocked yet");
        } else if (!isLocked() && locked) {
            throw new AssertionError("vault file has already been unlocked");
        }
    }

    private void assertLoaded(boolean loaded) {
        if (isLoaded() && !loaded) {
            throw new AssertionError("vault file has already been loaded");
        } else if (!isLoaded() && loaded) {
            throw new AssertionError("vault file has not been loaded yet");
        }
    }
}
