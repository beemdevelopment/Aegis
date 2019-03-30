package com.beemdevelopment.aegis.importers;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseFile;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.DatabaseFileException;
import com.beemdevelopment.aegis.encoding.Base64Exception;
import com.beemdevelopment.aegis.otp.OtpInfoException;
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
    public DatabaseImporterResult convert() throws DatabaseImporterException {
        DatabaseImporterResult result = new DatabaseImporterResult();

        try {
            JSONObject obj;
            if (_file.isEncrypted() && _creds != null) {
                obj = _file.getContent(_creds);
            } else {
                obj = _file.getContent();
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                JSONObject entryObj = array.getJSONObject(i);
                try {
                    DatabaseEntry entry = convertEntry(entryObj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }
        } catch (JSONException | DatabaseFileException e) {
            throw new DatabaseImporterException(e);
        }

        return result;
    }

    private static DatabaseEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
        try {
            return DatabaseEntry.fromJson(obj);
        } catch (JSONException | OtpInfoException | Base64Exception e) {
            throw new DatabaseImporterEntryException(e, obj.toString());
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
