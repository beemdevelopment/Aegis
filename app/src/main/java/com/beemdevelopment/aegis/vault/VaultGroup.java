package com.beemdevelopment.aegis.vault;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class VaultGroup extends UUIDMap.Value {
    private String _name;

    private VaultGroup(UUID uuid, String name) {
        super(uuid);
        _name = name;
    }

    public VaultGroup(String name) {
        super();
        _name = name;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("uuid", getUUID().toString());
            obj.put("name", _name);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static VaultGroup fromJson(JSONObject obj) throws VaultEntryException {
        try {
            UUID uuid = UUID.fromString(obj.getString("uuid"));
            String groupName = obj.getString("name");

            return new VaultGroup(uuid, groupName);
        } catch (JSONException e) {
            throw new VaultEntryException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VaultGroup)) {
            return false;
        }

        VaultGroup entry = (VaultGroup) o;
        return super.equals(entry) && getName().equals(entry.getName());
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    @Override
    public String toString() {
        return _name;
    }
}
