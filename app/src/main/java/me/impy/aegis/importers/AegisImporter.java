package me.impy.aegis.importers;

import org.json.JSONObject;

import java.util.List;

import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseException;
import me.impy.aegis.db.DatabaseFile;
import me.impy.aegis.db.DatabaseFileException;
import me.impy.aegis.util.ByteInputStream;

public class AegisImporter extends DatabaseImporter {
    private MasterKey _key;
    private DatabaseFile _file;

    public AegisImporter(ByteInputStream stream) {
        super(stream);
    }

    @Override
    public void parse() throws DatabaseImporterException {
        try {
            byte[] bytes = _stream.getBytes();
            _file = new DatabaseFile();
            _file.deserialize(bytes);
        } catch (DatabaseFileException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        try {
            JSONObject obj;
            if (_file.isEncrypted() && _key != null) {
                obj = _file.getContent(_key);
            } else {
                obj = _file.getContent();
            }

            Database db = new Database();
            db.deserialize(obj);
            return db.getKeys();
        } catch (DatabaseException | DatabaseFileException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public boolean isEncrypted() {
        return _file.isEncrypted();
    }

    public void setKey(MasterKey key) {
        _key = key;
    }

    public DatabaseFile getFile() {
        return _file;
    }

    @Override
    public String getName() {
        return "Aegis";
    }
}
