package com.beemdevelopment.aegis.vault;

import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;
import java.util.UUID;

public class Vault {
    private static final int VERSION = 3;
    private final UUIDMap<VaultEntry> _entries = new UUIDMap<>();
    private final UUIDMap<VaultGroup> _groups = new UUIDMap<>();
    private boolean _iconsOptimized = true;

    // Whether we've migrated the group list to the new format while parsing the vault
    private boolean _isGroupsMigrationFresh = false;

    public JSONObject toJson() {
        return toJson(null);
    }

    public JSONObject toJson(@Nullable EntryFilter filter) {
        try {
            JSONArray entriesArray = new JSONArray();
            for (VaultEntry e : _entries) {
                if (filter == null || filter.includeEntry(e)) {
                    entriesArray.put(e.toJson());
                }
            }

            // Always include all groups, even if they're not assigned to any entry (before or after the entry filter)
            JSONArray groupsArray = new JSONArray();
            for (VaultGroup group : _groups) {
                groupsArray.put(group.toJson());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", entriesArray);
            obj.put("groups", groupsArray);
            obj.put("icons_optimized", _iconsOptimized);

            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Vault fromJson(JSONObject obj) throws VaultException {
        Vault vault = new Vault();
        UUIDMap<VaultEntry> entries = vault.getEntries();
        UUIDMap<VaultGroup> groups = vault.getGroups();

        try {
            int ver = obj.getInt("version");
            if (ver > VERSION) {
                throw new VaultException("Unsupported version");
            }

            if (obj.has("groups")) {
                JSONArray groupsArray = obj.getJSONArray("groups");
                for (int i = 0; i < groupsArray.length(); i++) {
                    VaultGroup group = VaultGroup.fromJson(groupsArray.getJSONObject(i));
                    if (!groups.has(group)) {
                        groups.add(group);
                    }
                }
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                VaultEntry entry = VaultEntry.fromJson(array.getJSONObject(i));
                if (vault.migrateOldGroup(entry)) {
                    vault.setGroupsMigrationFresh();
                }

                // check the vault has a group corresponding to each one the entry claims to be in
                for (UUID groupUuid: entry.getGroups()) {
                    if (!groups.has(groupUuid)) {
                        entry.removeGroup(groupUuid);
                    }
                }

                entries.add(entry);
            }

            if (!obj.optBoolean("icons_optimized")) {
                vault.setIconsOptimized(false);
            }
        } catch (VaultEntryException | JSONException e) {
            throw new VaultException(e);
        }

        return vault;
    }

    private void setGroupsMigrationFresh() {
        _isGroupsMigrationFresh = true;
    }

    public boolean isGroupsMigrationFresh() {
        return _isGroupsMigrationFresh;
    }

    public void setIconsOptimized(boolean optimized) {
        _iconsOptimized = optimized;
    }

    public boolean areIconsOptimized() {
        return _iconsOptimized;
    }

    public boolean migrateOldGroup(VaultEntry entry) {
        if (entry.getOldGroup() != null) {
            Optional<VaultGroup> optGroup = getGroups().getValues()
                                                 .stream()
                                                 .filter(g -> g.getName().equals(entry.getOldGroup()))
                                                 .findFirst();

            if (optGroup.isPresent()) {
                entry.addGroup(optGroup.get().getUUID());
            } else {
                VaultGroup group = new VaultGroup(entry.getOldGroup());
                getGroups().add(group);
                entry.addGroup(group.getUUID());
            }

            entry.setOldGroup(null);
            return true;
        }

        return false;
    }

    public UUIDMap<VaultEntry> getEntries() {
        return _entries;
    }

    public UUIDMap<VaultGroup> getGroups() {
        return _groups;
    }

    public interface EntryFilter {
        boolean includeEntry(VaultEntry entry);
    }
}
