package me.impy.aegis.importers;

import org.json.JSONObject;

import java.util.List;

import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseException;
import me.impy.aegis.db.DatabaseFile;
import me.impy.aegis.db.DatabaseFileCredentials;
import me.impy.aegis.db.DatabaseFileException;
import me.impy.aegis.util.ByteInputStream;

public class AegisImporter extends DatabaseImporter {
    private DatabaseFileCredentials _creds;
    private DatabaseFile _file;

    public AegisImporter(ByteInputStream stream) {
        super(stream);
    }

    @Override
    public void parse() throws DatabaseImporterException {
        try {
            byte[] bytes = _stream.getBytes();
            _file = DatabaseFile.fromBytes(bytes);
        } catch (DatabaseFileException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        try {
            JSONObject obj;
            if (_file.isEncrypted() && _creds != null) {
                obj = _file.getContent(_creds);
            } else {
                obj = _file.getContent();
            }

            Database db = Database.fromJson(obj);
            return db.getEntries();
        } catch (DatabaseException | DatabaseFileException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public boolean isEncrypted() {
        return _file.isEncrypted();
    }

    public void setCredentials(DatabaseFileCredentials creds) {
        _creds = creds;
    }

    public DatabaseFile getFile() {
        return _file;
    }
}
