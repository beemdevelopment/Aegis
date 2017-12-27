package me.impy.aegis.db;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Database {
    private static final int VERSION = 1;

    private List<DatabaseEntry> _entries = new ArrayList<>();
    private long _counter = 0;

    public byte[] serialize() throws Exception {
        JSONArray array = new JSONArray();
        for (DatabaseEntry e : _entries) {
            array.put(e.serialize());
        }

        JSONObject obj = new JSONObject();
        obj.put("version", VERSION);
        obj.put("entries", array);

        return obj.toString().getBytes("UTF-8");
    }

    public void deserialize(byte[] data) throws Exception {
        JSONObject obj = new JSONObject(new String(data, "UTF-8"));

        // TODO: support different VERSION deserialization providers
        int ver = obj.getInt("version");
        if (ver != VERSION) {
            throw new Exception("Unsupported version");
        }

        JSONArray array = obj.getJSONArray("entries");
        for (int i = 0; i < array.length(); i++) {
            DatabaseEntry entry = new DatabaseEntry(null);
            entry.deserialize(array.getJSONObject(i));
            addKey(entry);
        }
    }

    public void addKey(DatabaseEntry entry) throws Exception {
        entry.setID(++_counter);
        _entries.add(entry);
    }

    public void removeKey(DatabaseEntry entry) {
        _entries.remove(entry);
    }

    public void replaceKey(DatabaseEntry newEntry) {
        for (DatabaseEntry oldEntry : _entries) {
            if (oldEntry.getID() == newEntry.getID()) {
                _entries.set(_entries.indexOf(oldEntry), newEntry);
                return;
            }
        }
        throw new AssertionError("no entry found with the same id");
    }

    public void swapKeys(DatabaseEntry entry1, DatabaseEntry entry2) {
        Collections.swap(_entries, _entries.indexOf(entry1), _entries.indexOf(entry2));
    }

    public List<DatabaseEntry> getKeys() {
        return Collections.unmodifiableList(_entries);
    }
}
