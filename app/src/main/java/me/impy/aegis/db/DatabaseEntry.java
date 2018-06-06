package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;

import me.impy.aegis.otp.OtpInfo;
import me.impy.aegis.otp.OtpInfoException;

public class DatabaseEntry implements Serializable {
    private UUID _uuid;
    private String _name = "";
    private String _issuer = "";
    private String _icon = "";
    private OtpInfo _info;

    public DatabaseEntry(OtpInfo info) {
        _info = info;
        _uuid = UUID.randomUUID();
    }

    public DatabaseEntry(OtpInfo info, String name, String issuer) {
        this(info);
        setName(name);
        setIssuer(issuer);
    }

    public JSONObject serialize() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type", _info.getType());
            obj.put("uuid", _uuid.toString());
            obj.put("name", _name);
            obj.put("issuer", _issuer);
            obj.put("info", _info.toJson());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public void deserialize(JSONObject obj) throws JSONException, OtpInfoException {
        // if there is no uuid, generate a new one
        if (!obj.has("uuid")) {
            _uuid = UUID.randomUUID();
        } else {
            _uuid = UUID.fromString(obj.getString("uuid"));
        }
        _name = obj.getString("name");
        _issuer = obj.getString("issuer");
        _info = OtpInfo.parseJson(obj.getString("type"), obj.getJSONObject("info"));
    }

    public UUID getUUID() {
        return _uuid;
    }

    public String getName() {
        return _name;
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getIcon() {
        return _icon;
    }

    public OtpInfo getInfo() {
        return _info;
    }

    public void setName(String name) {
        _name = name;
    }

    public void setIssuer(String issuer) {
        _issuer = issuer;
    }

    public void setIcon(String icon) {
        _icon = icon;
    }

    public void setInfo(OtpInfo info) {
        _info = info;
    }
}
