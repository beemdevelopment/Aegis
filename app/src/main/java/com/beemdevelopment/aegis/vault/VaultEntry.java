package com.beemdevelopment.aegis.vault;

import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.JsonUtils;
import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class VaultEntry extends UUIDMap.Value {
    private String _name = "";
    private String _issuer = "";
    private String _group;
    private OtpInfo _info;
    private byte[] _icon;
    private IconType _iconType = IconType.INVALID;
    private boolean _isFavorite;
    private int _usageCount;
    private String _note = "";

    private VaultEntry(UUID uuid, OtpInfo info) {
        super(uuid);
        _info = info;
    }

    public VaultEntry(OtpInfo info) {
        super();
        _info = info;
    }

    public VaultEntry(OtpInfo info, String name, String issuer) {
        this(info);
        setName(name);
        setIssuer(issuer);
    }

    public VaultEntry(OtpInfo info, String name, String issuer, String group) {
        this(info);
        setName(name);
        setIssuer(issuer);
        setGroup(group);
    }

    public VaultEntry(GoogleAuthInfo info) {
        this(info.getOtpInfo(), info.getAccountName(), info.getIssuer());
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type", _info.getTypeId());
            obj.put("uuid", getUUID().toString());
            obj.put("name", _name);
            obj.put("issuer", _issuer);
            obj.put("group", _group);
            obj.put("note", _note);
            obj.put("favorite", _isFavorite);
            obj.put("icon", _icon == null ? JSONObject.NULL : Base64.encode(_icon));
            obj.put("icon_mime", _icon == null ? null : _iconType.toMimeType());
            obj.put("info", _info.toJson());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static VaultEntry fromJson(JSONObject obj) throws VaultEntryException {
        try {
            // if there is no uuid, generate a new one
            UUID uuid;
            if (!obj.has("uuid")) {
                uuid = UUID.randomUUID();
            } else {
                uuid = UUID.fromString(obj.getString("uuid"));
            }

            OtpInfo info = OtpInfo.fromJson(obj.getString("type"), obj.getJSONObject("info"));
            VaultEntry entry = new VaultEntry(uuid, info);
            entry.setName(obj.getString("name"));
            entry.setIssuer(obj.getString("issuer"));
            entry.setGroup(obj.optString("group", null));
            entry.setNote(obj.optString("note", ""));
            entry.setIsFavorite(obj.optBoolean("favorite", false));

            Object icon = obj.get("icon");
            if (icon != JSONObject.NULL) {
                String mime = JsonUtils.optString(obj, "icon_mime");

                IconType iconType = mime == null ? IconType.JPEG : IconType.fromMimeType(mime);
                if (iconType == IconType.INVALID) {
                    throw new VaultEntryException(String.format("Bad icon MIME type: %s", mime));
                }

                byte[] iconBytes = Base64.decode((String) icon);
                entry.setIcon(iconBytes, iconType);
            }

            return entry;
        } catch (OtpInfoException | JSONException | EncodingException e) {
            throw new VaultEntryException(e);
        }
    }

    public String getName() {
        return _name;
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getGroup() {
        return _group;
    }

    public byte[] getIcon() {
        return _icon;
    }

    public IconType getIconType() {
        return _iconType;
    }

    public OtpInfo getInfo() {
        return _info;
    }

    public int getUsageCount() {
        return _usageCount;
    }

    public String getNote() { return _note; }

    public boolean isFavorite() { return _isFavorite; };

    public void setName(String name) {
        _name = name;
    }

    public void setIssuer(String issuer) {
        _issuer = issuer;
    }

    public void setGroup(String group) {
        _group = group;
    }

    public void setInfo(OtpInfo info) {
        _info = info;
    }

    public void setIcon(byte[] icon, IconType iconType) {
        _icon = icon;
        _iconType = iconType;
    }

    public boolean hasIcon() {
        return _icon != null;
    }

    public void setUsageCount(int usageCount) { _usageCount = usageCount; }

    public void setNote(String note) { _note = note; }

    public void setIsFavorite(boolean isFavorite) { _isFavorite = isFavorite; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VaultEntry)) {
            return false;
        }

        VaultEntry entry = (VaultEntry) o;
        return super.equals(entry) && equivalates(entry);
    }

    /**
     * Reports whether this entry is equivalent to the given entry. The UUIDs of these
     * entries are ignored during the comparison, so they are not necessarily the same
     * instance.
     */
    public boolean equivalates(VaultEntry entry) {
        return getName().equals(entry.getName())
                && getIssuer().equals(entry.getIssuer())
                && Objects.equals(getGroup(), entry.getGroup())
                && getInfo().equals(entry.getInfo())
                && Arrays.equals(getIcon(), entry.getIcon())
                && getIconType().equals(entry.getIconType())
                && getNote().equals(entry.getNote())
                && isFavorite() == entry.isFavorite();
    }

    /**
     * Reports whether this entry has its values set to the defaults.
     */
    public boolean isDefault() {
        return equivalates(getDefault());
    }

    public static VaultEntry getDefault() {
        try {
            return new VaultEntry(new TotpInfo(null));
        } catch (OtpInfoException e) {
            throw new RuntimeException(e);
        }
    }
}
