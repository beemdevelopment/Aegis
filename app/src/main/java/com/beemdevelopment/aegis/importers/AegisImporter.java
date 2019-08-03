package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseFile;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.DatabaseFileException;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.beemdevelopment.aegis.encoding.Base64Exception;
import com.beemdevelopment.aegis.otp.OtpInfoException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AegisImporter extends DatabaseImporter {

    public AegisImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            byte[] bytes = reader.readAll();
            DatabaseFile file = DatabaseFile.fromBytes(bytes);
            if (file.isEncrypted()) {
                return new EncryptedState(file);
            }
            return new DecryptedState(file.getContent());
        } catch (DatabaseFileException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends State {
        private DatabaseFile _file;

        private EncryptedState(DatabaseFile file) {
            super(true);
            _file = file;
        }

        public SlotList getSlots() {
            return _file.getHeader().getSlots();
        }

        public State decrypt(DatabaseFileCredentials creds) throws DatabaseFileException {
            JSONObject obj = _file.getContent(creds);
            return new DecryptedState(obj);
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {

        }
    }

    public static class DecryptedState extends State {
        private JSONObject _obj;

        private DecryptedState(JSONObject obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                JSONArray array = _obj.getJSONArray("entries");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject entryObj = array.getJSONObject(i);
                    try {
                        DatabaseEntry entry = convertEntry(entryObj);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
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
    }
}
