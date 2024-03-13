package com.beemdevelopment.aegis.icons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.util.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.io.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class IconPack {
    private UUID _uuid;
    private String _name;
    private int _version;
    private List<Icon> _icons;

    private File _dir;

    private IconPack(UUID uuid, String name, int version, List<Icon> icons) {
        _uuid = uuid;
        _name = name;
        _version = version;
        _icons = icons;
    }

    public UUID getUUID() {
        return _uuid;
    }

    public String getName() {
        return _name;
    }

    public int getVersion() {
        return _version;
    }

    public List<Icon> getIcons() {
        return Collections.unmodifiableList(_icons);
    }

    /**
     * Retrieves a list of icons suggested for the given issuer.
     */
    public List<Icon> getSuggestedIcons(String issuer) {
        if (issuer == null || issuer.isEmpty()) {
            return new ArrayList<>();
        }

        List<Icon> icons = new ArrayList<>();
        for (Icon icon : _icons) {
            MatchType matchType = icon.getMatchFor(issuer);
            if (matchType != null) {
                // Inverse matches (entry issuer contains icon name) are less likely
                // to be good, so position them at the end of the list.
                if (matchType.equals(MatchType.NORMAL)) {
                    icons.add(0, icon);
                } else if (matchType.equals(MatchType.INVERSE)) {
                    icons.add(icon);
                }
            }
        }

        return icons;
    }

    @Nullable
    public File getDirectory() {
        return _dir;
    }

    void setDirectory(@NonNull File dir) {
        _dir = dir;
    }

    /**
     * Indicates whether some other object is "equal to" this one. The object does not
     * necessarily have to be the same instance. Equality of UUID and version will make
     * this method return true;
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IconPack)) {
            return false;
        }

        IconPack pack = (IconPack) o;
        return super.equals(pack) || (getUUID().equals(pack.getUUID()) && getVersion() == pack.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_uuid, _version);
    }

    public static IconPack fromJson(JSONObject obj) throws JSONException {
        UUID uuid;
        String uuidString = obj.getString("uuid");
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new JSONException(String.format("Bad UUID format: %s", uuidString));
        }
        String name = obj.getString("name");
        int version = obj.getInt("version");
        JSONArray array = obj.getJSONArray("icons");

        List<Icon> icons = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Icon icon = Icon.fromJson(array.getJSONObject(i));
            icons.add(icon);
        }

        return new IconPack(uuid, name, version, icons);
    }

    public static IconPack fromBytes(byte[] data) throws JSONException {
        JSONObject obj = new JSONObject(new String(data, StandardCharsets.UTF_8));
        return IconPack.fromJson(obj);
    }

    public static class Icon implements Serializable {
        private final String _relFilename;
        private final String _name;
        private final String _category;
        private final List<String> _issuers;

        private File _file;

        protected Icon(String filename, String name, String category, List<String> issuers) {
            _relFilename = filename;
            _name = name;
            _category = category;
            _issuers = issuers;
        }

        public String getRelativeFilename() {
            return _relFilename;
        }

        @Nullable
        public File getFile() {
            return _file;
        }

        void setFile(@NonNull File file) {
            _file = file;
        }

        public IconType getIconType() {
            return IconType.fromFilename(_relFilename);
        }

        public String getName() {
            if (_name != null) {
                return _name;
            }
            return Files.getNameWithoutExtension(new File(_relFilename).getName());
        }

        public String getCategory() {
            return _category;
        }

        private MatchType getMatchFor(String issuer) {
            String lowerEntryIssuer = issuer.toLowerCase();

            boolean inverseMatch = false;
            for (String is : _issuers) {
                String lowerIconIssuer = is.toLowerCase();
                if (lowerIconIssuer.contains(lowerEntryIssuer)) {
                    return MatchType.NORMAL;
                }
                if (lowerEntryIssuer.contains(lowerIconIssuer)) {
                    inverseMatch = true;
                }
            }
            if (inverseMatch) {
                return MatchType.INVERSE;
            }

            return null;
        }

        public static Icon fromJson(JSONObject obj) throws JSONException {
            String filename = obj.getString("filename");
            String name = JsonUtils.optString(obj, "name");
            String category = obj.isNull("category") ? null : obj.getString("category");
            JSONArray array = obj.getJSONArray("issuer");

            List<String> issuers = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                String issuer = array.getString(i);
                issuers.add(issuer);
            }

            return new Icon(filename, name, category, issuers);
        }
    }

    private enum MatchType {
        NORMAL,
        INVERSE
    }
}
