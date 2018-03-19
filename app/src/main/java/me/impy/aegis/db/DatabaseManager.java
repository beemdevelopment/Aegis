package me.impy.aegis.db;

import android.content.Context;
import android.os.Environment;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import me.impy.aegis.BuildConfig;
import me.impy.aegis.crypto.MasterKey;

public class DatabaseManager {
    private static final String FILENAME = "aegis.json";
    private static final String FILENAME_EXPORT = "aegis_export.json";
    private static final String FILENAME_EXPORT_PLAIN = "aegis_export_plain.json";

    private MasterKey _key;
    private DatabaseFile _file;
    private Database _db;
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

        try {
            byte[] fileBytes;
            FileInputStream file = null;

            try {
                file = _context.openFileInput(FILENAME);
                fileBytes = new byte[(int) file.getChannel().size()];
                DataInputStream stream = new DataInputStream(file);
                stream.readFully(fileBytes);
                stream.close();
            } finally {
                // always close the file stream
                // there is no need to close the DataInputStream
                if (file != null) {
                    file.close();
                }
            }

            _file = new DatabaseFile();
            _file.deserialize(fileBytes);

            if (!_file.isEncrypted()) {
                JSONObject obj = _file.getContent();
                _db = new Database();
                _db.deserialize(obj);
            }
        } catch (IOException | DatabaseFileException | DatabaseException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void lock() {
        assertState(false, true);
        // TODO: properly clear everything
        _key = null;
        _db = null;
    }

    public void unlock(MasterKey key) throws DatabaseManagerException {
        assertState(true, true);

        try {
            JSONObject obj = _file.getContent(key);
            _db = new Database();
            _db.deserialize(obj);
            _key = key;
        } catch (DatabaseFileException | DatabaseException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public static void save(Context context, DatabaseFile file) throws DatabaseManagerException {
        try {
            byte[] bytes = file.serialize();

            FileOutputStream stream = null;
            try {
                stream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                stream.write(bytes);
            } finally {
                // always close the file stream
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException | DatabaseFileException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void save() throws DatabaseManagerException {
        assertState(false, true);

        try {
            JSONObject obj = _db.serialize();
            if (_file.isEncrypted()) {
                _file.setContent(obj, _key);
            } else {
                _file.setContent(obj);
            }
            save(_context, _file);
        } catch (DatabaseException | DatabaseFileException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public String export(boolean encrypt) throws DatabaseManagerException {
        assertState(false, true);

        try {
            DatabaseFile dbFile = new DatabaseFile();
            dbFile.setSlots(_file.getSlots());
            if (encrypt && getFile().isEncrypted()) {
                dbFile.setContent(_db.serialize(), _key);
            } else {
                dbFile.setContent(_db.serialize());
            }

            File file;
            FileOutputStream stream = null;
            try {
                String dirName = !BuildConfig.DEBUG ? "Aegis" : "AegisDebug";
                File dir = new File(Environment.getExternalStorageDirectory(), dirName);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IOException("error creating external storage directory");
                }

                byte[] bytes = dbFile.serialize();
                file = new File(dir.getAbsolutePath(), encrypt ? FILENAME_EXPORT : FILENAME_EXPORT_PLAIN);
                stream = new FileOutputStream(file);
                stream.write(bytes);
            } finally {
                // always close the file stream
                if (stream != null) {
                    stream.close();
                }
            }

            return file.getAbsolutePath();
        } catch (DatabaseException | IOException | DatabaseFileException e) {
            throw new DatabaseManagerException(e);
        }
    }

    public void addKey(DatabaseEntry entry) {
        assertState(false, true);
        _db.addKey(entry);
    }

    public void removeKey(DatabaseEntry entry) {
        assertState(false, true);
        _db.removeKey(entry);
    }

    public void replaceKey(DatabaseEntry entry) {
        assertState(false, true);
        _db.replaceKey(entry);
    }

    public void swapKeys(DatabaseEntry entry1, DatabaseEntry entry2) {
        assertState(false, true);
        _db.swapKeys(entry1, entry2);
    }

    public List<DatabaseEntry> getKeys() {
        assertState(false, true);
        return _db.getKeys();
    }

    public MasterKey getMasterKey() {
        assertState(false, true);
        return _key;
    }

    public DatabaseFile getFile() {
        return _file;
    }

    public boolean isLoaded() {
        return _file != null;
    }

    public boolean isLocked() {
        return _db == null;
    }

    private void assertState(boolean locked, boolean loaded) {
        if (isLoaded() && !loaded) {
            throw new AssertionError("database file has not been loaded yet");
        } else if (!isLoaded() && loaded) {
            throw new AssertionError("database file has is already been loaded");
        }

        if (isLocked() && !locked) {
            throw new AssertionError("database file has not been unlocked yet");
        } else if (!isLocked() && locked) {
            throw new AssertionError("database file has is already been unlocked");
        }
    }
}
