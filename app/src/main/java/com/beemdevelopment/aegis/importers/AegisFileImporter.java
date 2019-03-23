package com.beemdevelopment.aegis.importers;

import android.content.Context;

import org.json.JSONObject;

import java.util.List;

import com.beemdevelopment.aegis.db.Database;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseException;
import com.beemdevelopment.aegis.db.DatabaseFile;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.DatabaseFileException;
import com.beemdevelopment.aegis.util.ByteInputStream;

public class AegisFileImporter extends DatabaseFileImporter {
    private DatabaseFileCredentials _creds;
    private DatabaseFile _file;

    public AegisFileImporter(Context context, ByteInputStream stream) {
        super(context, stream);
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
