package com.beemdevelopment.aegis.icons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.stream.Collectors;

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

        return _icons.stream()
                .filter(i -> i.isSuggestedFor(issuer))
                .collect(Collectors.toList());
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
        private final String _category;
        private final List<String> _issuers;

        private File _file;

        protected Icon(String filename, String category, List<String> issuers) {
            _relFilename = filename;
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
            return Files.getNameWithoutExtension(new File(_relFilename).getName());
        }

        public String getCategory() {
            return _category;
        }

        public List<String> getIssuers() {
            return Collections.unmodifiableList(_issuers);
        }

        public boolean isSuggestedFor(String issuer) {
            String lowerIssuer = issuer.toLowerCase();
            return getIssuers().stream()
                    .map(String::toLowerCase)
                    .anyMatch(is -> is.contains(lowerIssuer) || lowerIssuer.contains(is));
        }

        public static Icon fromJson(JSONObject obj) throws JSONException {
            String filename = obj.getString("filename");
            String category = obj.isNull("category") ? null : obj.getString("category");
            JSONArray array = obj.getJSONArray("issuer");

            List<String> issuers = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                String issuer = array.getString(i);
                issuers.add(issuer);
            }

            return new Icon(filename, category, issuers);
        }
    }
}
