package me.impy.aegis.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import me.impy.aegis.otp.OtpInfoException;

public class Database {
    private static final int VERSION = 1;

    private DatabaseEntryList _entries = new DatabaseEntryList();

    public JSONObject serialize() {
        try {
            JSONArray array = new JSONArray();
            for (DatabaseEntry e : _entries) {
                array.put(e.serialize());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", array);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void deserialize(JSONObject obj) throws DatabaseException {
        // TODO: support different VERSION deserialization providers
        try {
            int ver = obj.getInt("version");
            if (ver != VERSION) {
                throw new DatabaseException("Unsupported version");
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                DatabaseEntry entry = new DatabaseEntry(null);
                entry.deserialize(array.getJSONObject(i));
                addEntry(entry);
            }
        } catch (OtpInfoException | JSONException e) {
            throw new DatabaseException(e);
        }
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
}
