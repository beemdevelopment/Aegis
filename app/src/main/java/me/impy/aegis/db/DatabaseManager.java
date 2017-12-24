package me.impy.aegis.db;

import android.content.Context;
import android.os.Environment;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import me.impy.aegis.BuildConfig;
import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;

public class DatabaseManager {
    private static final String FILENAME = "aegis.db";
    private static final String FILENAME_EXPORT = "aegis_export.db";
    private static final String FILENAME_EXPORT_PLAIN = "aegis_export.json";

    private MasterKey _key;
    private DatabaseFile _file;
    private Database _db;
    private Context _context;

    public DatabaseManager(Context context) {
        _context = context;
    }

    public void load() throws Exception {
        assertState(true, false);

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
            byte[] contentBytes = _file.getContent();
            _db = new Database();
            _db.deserialize(contentBytes);
        }
    }

    public void lock() throws Exception {
        assertState(false, true);
        // TODO: properly clear everything
        _key = null;
        _db = null;
    }

    public void unlock(MasterKey key) throws Exception {
        assertState(true, true);
        byte[] encrypted = _file.getContent();
        CryptParameters params = _file.getCryptParameters();
        CryptResult result = key.decrypt(encrypted, params);
        _db = new Database();
        _db.deserialize(result.Data);
        _key = key;
    }

    public static void save(Context context, DatabaseFile file) throws IOException {
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
    }

    public void save() throws Exception {
        assertState(false, true);
        byte[] dbBytes = _db.serialize();
        if (!_file.isEncrypted()) {
            _file.setContent(dbBytes);
        } else {
            CryptResult result = _key.encrypt(dbBytes);
            _file.setContent(result.Data);
            _file.setCryptParameters(result.Parameters);
        }
        save(_context, _file);
    }

    public String export(boolean encrypt) throws Exception {
        assertState(false, true);
        byte[] bytes = _db.serialize();
        encrypt = encrypt && getFile().isEncrypted();
        if (encrypt) {
            CryptResult result = _key.encrypt(bytes);
            _file.setContent(result.Data);
            _file.setCryptParameters(result.Parameters);
            bytes = _file.serialize();
        }

        File file;
        FileOutputStream stream = null;
        try {
            String dirName = !BuildConfig.DEBUG ? "Aegis" : "AegisDebug";
            File dir = new File(Environment.getExternalStorageDirectory(), dirName);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("error creating external storage directory");
            }

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
    }

    public void addKey(DatabaseEntry entry) throws Exception {
        assertState(false, true);
        _db.addKey(entry);
    }

    public void removeKey(DatabaseEntry entry) throws Exception {
        assertState(false, true);
        _db.removeKey(entry);
    }

    public void swapKeys(DatabaseEntry entry1, DatabaseEntry entry2) throws Exception {
        assertState(false, true);
        _db.swapKeys(entry1, entry2);
    }

    public List<DatabaseEntry> getKeys() throws Exception {
        assertState(false, true);
        return _db.getKeys();
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

    private void assertState(boolean locked, boolean loaded) throws Exception {
        if (isLoaded() && !loaded) {
            throw new Exception("database file has not been loaded yet");
        } else if (!isLoaded() && loaded) {
            throw new Exception("database file has is already been loaded");
        }

        if (isLocked() && !locked) {
            throw new Exception("database file has not been unlocked yet");
        } else if (!isLocked() && locked) {
            throw new Exception("database file has is already been unlocked");
        }
    }
}
