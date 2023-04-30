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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class VaultEntry extends UUIDMap.Value {
    private String _name = "";
    private String _issuer = "";
    private OtpInfo _info;
    private byte[] _icon;
    private IconType _iconType = IconType.INVALID;
    private boolean _isFavorite;
    private int _usageCount;
    private String _note = "";
    private String _oldGroup;
    private Set<UUID> _groups = new TreeSet<>();

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
            obj.put("note", _note);
            obj.put("favorite", _isFavorite);
            obj.put("icon", _icon == null ? JSONObject.NULL : Base64.encode(_icon));
            obj.put("icon_mime", _icon == null ? null : _iconType.toMimeType());
            obj.put("info", _info.toJson());

            JSONArray groupUuids = new JSONArray();
            for (UUID uuid : _groups) {
                groupUuids.put(uuid.toString());
            }
            obj.put("groups", groupUuids);

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
            entry.setNote(obj.optString("note", ""));
            entry.setIsFavorite(obj.optBoolean("favorite", false));

            // If the entry contains a list of group UUID's, assume conversion from the
            // old group system has already taken place and ignore the old group field.
            if (obj.has("groups")) {
                JSONArray groups = obj.getJSONArray("groups");
                for (int i = 0; i < groups.length(); i++) {
                    String groupUuid = groups.getString(i);
                    entry.addGroup(UUID.fromString(groupUuid));
                }
            } else if (obj.has("group")) {
                entry.setOldGroup(JsonUtils.optString(obj, "group"));
            }

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

    public Set<UUID> getGroups() {
        return _groups;
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

    public void addGroup(UUID group) {
        if (group == null) {
            throw new AssertionError("Attempt to add null group to entry's group list");
        }
        _groups.add(group);
    }

    public void removeGroup(UUID group) {
        _groups.remove(group);
    }

    public void setGroups(Set<UUID> groups) {
        if (groups.contains(null)) {
            throw new AssertionError("Attempt to add null group to entry's group list");
        }
        _groups = groups;
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

    void setOldGroup(String oldGroup) {
        _oldGroup = oldGroup;
    }

    String getOldGroup() {
        return _oldGroup;
    }

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
                && getInfo().equals(entry.getInfo())
                && Arrays.equals(getIcon(), entry.getIcon())
                && getIconType().equals(entry.getIconType())
                && getNote().equals(entry.getNote())
                && isFavorite() == entry.isFavorite()
                && getGroups().equals(entry.getGroups());
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
