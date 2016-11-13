package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

public class DatabaseEntry {
    public int ID;
    public String Name;
    public String URL;
    public int Order;

    public JSONObject serialize() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", ID);
        obj.put("name", Name);
        obj.put("url", URL);
        obj.put("order", Order);
        return obj;
    }

    public void deserialize(JSONObject obj) throws JSONException {
        ID = obj.getInt("id");
        Name = obj.getString("name");
        URL = obj.getString("url");
        Order = obj.getInt("order");
    }
}
