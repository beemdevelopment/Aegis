package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import me.impy.aegis.crypto.KeyInfo;

public class DatabaseEntry implements Serializable {
    public String _name = "";
    public String _icon = "";
    public KeyInfo _info;

    public DatabaseEntry(KeyInfo info) {
        _info = info;
    }

    public JSONObject serialize() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", _name);
        obj.put("url", _info.getURL());
        return obj;
    }

    public void deserialize(JSONObject obj) throws Exception {
        _name = obj.getString("name");
        _info = KeyInfo.fromURL(obj.getString("url"));
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
