package com.beemdevelopment.aegis.db;

import com.beemdevelopment.aegis.encoding.Base64Exception;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Database {
    private static final int VERSION = 1;
    private UUIDMap<DatabaseEntry> _entries = new UUIDMap<>();

    public JSONObject toJson() {
        try {
            JSONArray array = new JSONArray();
            for (DatabaseEntry e : _entries) {
                array.put(e.toJson());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", array);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Database fromJson(JSONObject obj) throws DatabaseException {
        Database db = new Database();
        UUIDMap<DatabaseEntry> entries = db.getEntries();

        try {
            int ver = obj.getInt("version");
            if (ver != VERSION) {
                throw new DatabaseException("Unsupported version");
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                DatabaseEntry entry = DatabaseEntry.fromJson(array.getJSONObject(i));
                entries.add(entry);
            }
        } catch (Base64Exception | OtpInfoException | JSONException e) {
            throw new DatabaseException(e);
        }

        return db;
    }

    public UUIDMap<DatabaseEntry> getEntries() {
        return _entries;
    }
}
