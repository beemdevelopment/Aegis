package me.impy.aegis.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import me.impy.aegis.crypto.KeyInfoException;

public class Database {
    private static final int VERSION = 1;

    private List<DatabaseEntry> _entries = new ArrayList<>();

    public JSONObject serialize() throws DatabaseException {
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
            throw new DatabaseException(e);
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
                addKey(entry);
            }
        } catch (JSONException | KeyInfoException e) {
            throw new DatabaseException(e);
        }
    }

    public void addKey(DatabaseEntry entry) {
        if (tryGetKeyByUUID(entry.getUUID()) != null) {
            throw new AssertionError("entry found with the same uuid");
        }
        _entries.add(entry);
    }

    public void removeKey(DatabaseEntry entry) {
        entry = getKeyByUUID(entry.getUUID());
        _entries.remove(entry);
    }

    public void replaceKey(DatabaseEntry newEntry) {
        DatabaseEntry oldEntry = getKeyByUUID(newEntry.getUUID());
        _entries.set(_entries.indexOf(oldEntry), newEntry);
    }

    public void swapKeys(DatabaseEntry entry1, DatabaseEntry entry2) {
        Collections.swap(_entries, _entries.indexOf(entry1), _entries.indexOf(entry2));
    }

    public List<DatabaseEntry> getKeys() {
        return Collections.unmodifiableList(_entries);
    }

    private DatabaseEntry tryGetKeyByUUID(UUID uuid) {
        for (DatabaseEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    private DatabaseEntry getKeyByUUID(UUID uuid) {
        DatabaseEntry entry = tryGetKeyByUUID(uuid);
        if (entry == null) {
            throw new AssertionError("no entry found with the same uuid");
        }
        return entry;
    }
}
