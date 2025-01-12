package com.beemdevelopment.aegis.vault;

import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.util.JsonUtils;
import com.google.common.hash.HashCode;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class VaultEntryIcon implements Serializable {
    private final byte[] _bytes;
    private final byte[] _hash;
    private final IconType _type;

    public static final int MAX_DIMENS = 512;

    public VaultEntryIcon(byte @NonNull [] bytes, @NonNull IconType type) {
        this(bytes, type, generateHash(bytes, type));
    }

    VaultEntryIcon(byte @NonNull [] bytes, @NonNull IconType type, byte @NonNull [] hash) {
        _bytes = bytes;
        _hash = hash;
        _type = type;
    }

    public byte @NonNull [] getBytes() {
        return _bytes;
    }

    public byte @NonNull [] getHash() {
        return _hash;
    }

    @NonNull
    public IconType getType() {
        return _type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VaultEntryIcon)) {
            return false;
        }

        VaultEntryIcon entry = (VaultEntryIcon) o;
        return Arrays.equals(getHash(), entry.getHash());
    }

    @Override
    public int hashCode() {
        return HashCode.fromBytes(_hash).asInt();
    }

    static void toJson(@Nullable VaultEntryIcon icon, @NonNull JSONObject obj) throws JSONException {
        obj.put("icon", icon == null ? JSONObject.NULL : Base64.encode(icon.getBytes()));
        if (icon != null) {
            obj.put("icon_mime", icon.getType().toMimeType());
            obj.put("icon_hash", Hex.encode(icon.getHash()));
        }
    }

    @Nullable
    static VaultEntryIcon fromJson(@NonNull JSONObject obj) throws VaultEntryIconException {
        try {
            Object icon = obj.get("icon");
            if (icon == JSONObject.NULL) {
                return null;
            }

            String mime = JsonUtils.optString(obj, "icon_mime");
            IconType iconType = mime == null ? IconType.JPEG : IconType.fromMimeType(mime);
            if (iconType == IconType.INVALID) {
                throw new VaultEntryIconException(String.format("Bad icon MIME type: %s", mime));
            }

            byte[] iconBytes = Base64.decode((String) icon);
            String iconHashStr = JsonUtils.optString(obj, "icon_hash");
            if (iconHashStr != null) {
                byte[] iconHash = Hex.decode(iconHashStr);
                return new VaultEntryIcon(iconBytes, iconType, iconHash);
            }

            return new VaultEntryIcon(iconBytes, iconType);
        } catch (JSONException | EncodingException e) {
            throw new VaultEntryIconException(e);
        }
    }

    private static byte @NonNull [] generateHash(byte @NonNull [] bytes, @NonNull IconType type) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(type.toMimeType().getBytes(StandardCharsets.UTF_8));
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
