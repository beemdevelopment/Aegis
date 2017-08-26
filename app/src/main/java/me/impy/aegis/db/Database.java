package me.impy.aegis.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.KeyProfile;
import me.impy.aegis.crypto.KeyInfo;

public class Database {
    private static final int version = 1;
    private List<DatabaseEntry> entries = new ArrayList<>();

    public byte[] serialize() throws JSONException, UnsupportedEncodingException {
        JSONArray array = new JSONArray();
        for (DatabaseEntry e : entries) {
            array.put(e.serialize());
        }

        JSONObject obj = new JSONObject();
        obj.put("version", version);
        obj.put("entries", array);

        return obj.toString().getBytes("UTF-8");
    }

    public void deserialize(byte[] data) throws Exception {
        JSONObject obj = new JSONObject(new String(data, "UTF-8"));

        // TODO: support different version deserialization providers
        int ver = obj.getInt("version");
        if (ver != version) {
            throw new Exception("Unsupported version");
        }

        JSONArray array = obj.getJSONArray("entries");
        for (int i = 0; i < array.length(); i++) {
            DatabaseEntry e = new DatabaseEntry();
            e.deserialize(array.getJSONObject(i));
            entries.add(e);
        }
    }

    public void addKey(KeyProfile profile) throws Exception {
        DatabaseEntry e = new DatabaseEntry();
        e.Name = profile.Name;
        e.URL = profile.Info.getURL();
        e.Order = profile.Order;
        e.ID = entries.size() + 1;
        profile.ID = e.ID;
        entries.add(e);
    }

    public void updateKey(KeyProfile profile) throws Exception {
        DatabaseEntry e = findEntry(profile);
        e.Name = profile.Name;
        e.URL = profile.Info.getURL();
        e.Order = profile.Order;
    }

    public void removeKey(KeyProfile profile) throws Exception {
        DatabaseEntry e = findEntry(profile);
        entries.remove(e);
    }

    public List<KeyProfile> getKeys() throws Exception {
        List<KeyProfile> list = new ArrayList<>();

        for (DatabaseEntry e : entries) {
            KeyProfile profile = new KeyProfile();
            profile.Name = e.Name;
            profile.Info = KeyInfo.fromURL(e.URL);
            profile.Order = e.Order;
            profile.ID = e.ID;
            list.add(profile);
        }

        return list;
    }

    private DatabaseEntry findEntry(KeyProfile profile) throws Exception {
        for (DatabaseEntry e : entries) {
            if (e.ID == profile.ID) {
                return e;
            }
        }

        throw new Exception("Key doesn't exist");
    }
}
