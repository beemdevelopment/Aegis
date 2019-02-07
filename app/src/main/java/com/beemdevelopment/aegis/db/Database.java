package com.beemdevelopment.aegis.db;

import com.beemdevelopment.aegis.encoding.Base64Exception;
import com.beemdevelopment.aegis.otp.OtpInfoException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class Database {
    private static final int VERSION = 1;
    private DatabaseEntryList _entries = new DatabaseEntryList();

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

        try {
            int ver = obj.getInt("version");
            if (ver != VERSION) {
                throw new DatabaseException("Unsupported version");
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                DatabaseEntry entry = DatabaseEntry.fromJson(array.getJSONObject(i));
                db.addEntry(entry);
            }
        } catch (Base64Exception | OtpInfoException | JSONException e) {
            throw new DatabaseException(e);
        }

        return db;
    }

    public void addEntry(DatabaseEntry entry) {
        _entries.add(entry);
    }

    public void removeEntry(DatabaseEntry entry) {
        _entries.remove(entry);
    }

    public void replaceEntry(DatabaseEntry newEntry) {
        _entries.replace(newEntry);
    }

    public void swapEntries(DatabaseEntry entry1, DatabaseEntry entry2) {
        _entries.swap(entry1, entry2);
    }

    public List<DatabaseEntry> getEntries() {
        return _entries.getList();
    }

    public DatabaseEntry getEntryByUUID(UUID uuid) {
        return _entries.getByUUID(uuid);
    }
}
