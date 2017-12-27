package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import me.impy.aegis.crypto.KeyInfo;

public class DatabaseEntry implements Serializable {
    private long _id = -1;
    private String _name = "";
    private String _icon = "";
    private KeyInfo _info;

    public DatabaseEntry(KeyInfo info) {
        _info = info;
    }

    public JSONObject serialize() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", _id);
        obj.put("name", _name);
        obj.put("url", _info.getURL());
        return obj;
    }

    public void deserialize(JSONObject obj, boolean ignoreID) throws Exception {
        if (!ignoreID) {
            _id = obj.getLong("id");
        }
        _name = obj.getString("name");
        _info = KeyInfo.fromURL(obj.getString("url"));
    }

    public long getID() {
        return _id;
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

    void setID(long id) throws Exception {
        if (_id != -1) {
            throw new Exception("this entry has already received an id");
        }
        _id = id;
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
