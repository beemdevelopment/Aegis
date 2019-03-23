package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;

public class DatabaseEntryGroup implements Serializable {
    private UUID _uuid;
    private String _name;

    private DatabaseEntryGroup(UUID uuid, String name) {
        _uuid = uuid;
        _name = name;
    }

    public DatabaseEntryGroup(String name) {
        this(UUID.randomUUID(), name);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("uuid", _uuid.toString());
            obj.put("name", _name);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static DatabaseEntryGroup fromJson(JSONObject obj) throws JSONException {
        // if there is no uuid, generate a new one
        UUID uuid;
        if (!obj.has("uuid")) {
            uuid = UUID.randomUUID();
        } else {
            uuid = UUID.fromString(obj.getString("uuid"));
        }

        return new DatabaseEntryGroup(uuid, obj.getString("name"));
    }

    public UUID getUUID() {
        return _uuid;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }
}
