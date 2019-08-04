package com.beemdevelopment.aegis.db;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.services.NotificationService;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;

public class DatabaseManager {
    private static final String FILENAME = "aegis.json";
    private static final String FILENAME_EXPORT = "aegis_export.json";
    private static final String FILENAME_EXPORT_PLAIN = "aegis_export_plain.json";

    private Database _db;
    private DatabaseFile _file;
    private DatabaseFileCredentials _creds;
    private boolean _encrypt;

    private Context _context;

    public DatabaseManager(Context context) {
        _context = context;
    }

    public boolean fileExists() {
        File file = new File(_context.getFilesDir(), FILENAME);
        return file.exists() && file.isFile();
    }

    public void load() throws DatabaseManagerException {
        assertState(true, false);

        try (FileInputStream file = _context.openFileInput(FILENAME)) {
            byte[] fileBytes = new byte[(int) file.getChannel().size()];
            DataInputStream stream = new DataInputStream(file);
            stream.readFully(fileBytes);
            stream.close();

            _file = DatabaseFile.fromBytes(fileBytes);
            _encrypt = _file.isEncrypted();
            if (!isEncryptionEnabled()) {
                JSONObject obj = _file.getContent();
                _db = Database.fromJson(obj);
            }
        } catch (IOException | DatabaseFileException | DatabaseException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void lock() {
        assertState(false, true);
        _creds = null;
        _db = null;
    }

    public void unlock(DatabaseFileCredentials creds) throws DatabaseManagerException {
        assertState(true, true);

        try {
            JSONObject obj = _file.getContent(creds);
            _db = Database.fromJson(obj);
            _creds = creds;
            _context.startService(new Intent(_context, NotificationService.class));
        } catch (DatabaseFileException | DatabaseException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public static void save(Context context, DatabaseFile file) throws DatabaseManagerException {
        byte[] bytes = file.toBytes();
        try (FileOutputStream stream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)) {
            stream.write(bytes);
        } catch (IOException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void save() throws DatabaseManagerException {
        assertState(false, true);

        try {
            JSONObject obj = _db.toJson();
            if (isEncryptionEnabled()) {
                _file.setContent(obj, _creds);
            } else {
                _file.setContent(obj);
            }
            save(_context, _file);
        } catch (DatabaseFileException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public String export(boolean encrypt) throws DatabaseManagerException {
        assertState(false, true);

        try {
            DatabaseFile dbFile = new DatabaseFile();
            if (encrypt && isEncryptionEnabled()) {
                dbFile.setContent(_db.toJson(), _creds);
            } else {
                dbFile.setContent(_db.toJson());
            }

            String dirName = !BuildConfig.DEBUG ? _context.getString(R.string.app_name) : _context.getString(R.string.app_name_dev);
            File dir = new File(Environment.getExternalStorageDirectory(), dirName);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("error creating external storage directory");
            }

            byte[] bytes = dbFile.toBytes();
            File file = new File(dir.getAbsolutePath(), encrypt ? FILENAME_EXPORT : FILENAME_EXPORT_PLAIN);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(bytes);
            }

            return file.getAbsolutePath();
        } catch (IOException | DatabaseFileException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void addEntry(DatabaseEntry entry) {
        assertState(false, true);
        _db.getEntries().add(entry);
    }

    public DatabaseEntry removeEntry(DatabaseEntry entry) {
        assertState(false, true);
        return _db.getEntries().remove(entry);
    }

    public DatabaseEntry replaceEntry(DatabaseEntry entry) {
        assertState(false, true);
        return _db.getEntries().replace(entry);
    }

    public void swapEntries(DatabaseEntry entry1, DatabaseEntry entry2) {
        assertState(false, true);
        _db.getEntries().swap(entry1, entry2);
    }

    public boolean isEntryDuplicate(DatabaseEntry entry) {
        assertState(false, true);
        return _db.getEntries().has(entry);
    }

    public Collection<DatabaseEntry> getEntries() {
        assertState(false, true);
        return _db.getEntries().getValues();
    }

    public TreeSet<String> getGroups() {
        assertState(false, true);

        TreeSet<String> groups = new TreeSet<>(Collator.getInstance());
        for (DatabaseEntry entry : getEntries()) {
            String group = entry.getGroup();
            if (group != null) {
                groups.add(group);
            }
        }
        return groups;
    }

    public DatabaseFileCredentials getCredentials() {
        assertState(false, true);
        return _creds;
    }

    public void setCredentials(DatabaseFileCredentials creds) {
        assertState(false, true);
        _creds = creds;
    }

    public DatabaseFile.Header getFileHeader() {
        assertLoaded(true);
        return _file.getHeader();
    }

    public boolean isEncryptionEnabled() {
        assertLoaded(true);
        return _encrypt;
    }

    public void enableEncryption(DatabaseFileCredentials creds) throws DatabaseManagerException {
        assertState(false, true);
        _creds = creds;
        _encrypt = true;
        save();
    }

    public void disableEncryption() throws DatabaseManagerException {
        assertState(false, true);
        _creds = null;
        _encrypt = false;
        save();
    }

    public boolean isLoaded() {
        return _file != null;
    }

    public boolean isLocked() {
        return _db == null;
    }

    private void assertState(boolean locked, boolean loaded) {
        assertLoaded(loaded);

        if (isLocked() && !locked) {
            throw new AssertionError("database file has not been unlocked yet");
        } else if (!isLocked() && locked) {
            throw new AssertionError("database file has already been unlocked");
        }
    }

    private void assertLoaded(boolean loaded) {
        if (isLoaded() && !loaded) {
            throw new AssertionError("database file has already been loaded");
        } else if (!isLoaded() && loaded) {
            throw new AssertionError("database file has not been loaded yet");
        }
    }
}
