package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.KeyInfoException;

public class DatabaseEntry implements Serializable {
    private UUID _uuid;
    private String _name = "";
    private String _icon = "";
    private KeyInfo _info;

    public DatabaseEntry() {
        this(new KeyInfo());
    }

    public DatabaseEntry(KeyInfo info) {
        _info = info;
        _uuid = UUID.randomUUID();
    }

    public JSONObject serialize() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("uuid", _uuid.toString());
        obj.put("name", _name);
        obj.put("url", _info.getURL());
        return obj;
    }

    public void deserialize(JSONObject obj) throws JSONException, KeyInfoException {
        // if there is no uuid, generate a new one
        if (!obj.has("uuid")) {
            _uuid = UUID.randomUUID();
        } else {
            _uuid = UUID.fromString(obj.getString("uuid"));
        }
        _name = obj.getString("name");
        _info = KeyInfo.fromURL(obj.getString("url"));
    }

    public UUID getUUID() {
        return _uuid;
    }
    public String getName() {
        return _name;
    }
    public String getIcon() {
        return _icon;
    }
    public KeyInfo getInfo() {
        return _info;
    }

    public void setName(String name) {
        _name = name;
    }
    public void setIcon(String icon) {
        _icon = icon;
    }
    public void setInfo(KeyInfo info) {
        _info = info;
    }
}
