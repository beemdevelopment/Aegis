package me.impy.aegis.db;

import android.content.Context;

import java.util.List;

import me.impy.aegis.KeyProfile;
import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;

public class DatabaseManager {
    private MasterKey _key;
    private DatabaseFile _file;
    private Database _db;
    private Context _context;

    public DatabaseManager(Context context) {
        _context = context;
    }

    public void load() throws Exception {
        _file = DatabaseFile.load(_context);
        if (!_file.isEncrypted()) {
            byte[] bytes = _file.getContent();
            _db = new Database();
            _db.deserialize(bytes);
        }
    }

    public void setMasterKey(MasterKey key) throws Exception {
        byte[] encrypted = _file.getContent();
        CryptParameters params = _file.getCryptParameters();
        CryptResult result = key.decrypt(encrypted, params);
        _db = new Database();
        _db.deserialize(result.Data);
        _key = key;
    }

    public void save() throws Exception {
        assertDecrypted();
        byte[] bytes = _db.serialize();
        if (!_file.isEncrypted()) {
            _file.setContent(bytes);
        } else {
            CryptResult result = _key.encrypt(bytes);
            _file.setContent(result.Data);
            _file.setCryptParameters(result.Parameters);
        }
        _file.save(_context);
    }

    public void addKey(KeyProfile profile) throws Exception {
        assertDecrypted();
        _db.addKey(profile);
    }

    public void updateKey(KeyProfile profile) throws Exception {
        assertDecrypted();
        _db.updateKey(profile);
    }

    public void removeKey(KeyProfile profile) throws Exception {
        assertDecrypted();
        _db.removeKey(profile);
    }

    public List<KeyProfile> getKeys() throws Exception {
        assertDecrypted();
        return _db.getKeys();
    }

    public DatabaseFile getFile() {
        return _file;
    }

    public boolean isLoaded() {
        return _file != null;
    }

    public boolean isDecrypted() {
        return _db != null;
    }

    private void assertLoaded() throws Exception {
        if (!isLoaded()) {
            throw new Exception("database file has not been loaded yet");
        }
    }

    private void assertDecrypted() throws Exception {
        assertLoaded();
        if (!isDecrypted()) {
            throw new Exception("database file has not been decrypted yet");
        }
    }
}
