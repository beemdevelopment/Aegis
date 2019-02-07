package com.beemdevelopment.aegis.db;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DatabaseEntryList implements Iterable<DatabaseEntry>, Serializable {
    private List<DatabaseEntry> _entries = new ArrayList<>();

    @NonNull
    @Override
    public Iterator<DatabaseEntry> iterator() {
        return _entries.iterator();
    }

    public void add(DatabaseEntry entry) {
        if (getByUUID(entry.getUUID()) != null) {
            throw new AssertionError("entry found with the same uuid");
        }
        _entries.add(entry);
    }

    public void remove(DatabaseEntry entry) {
        entry = mustGetByUUID(entry.getUUID());
        _entries.remove(entry);
    }

    public void replace(DatabaseEntry newEntry) {
        DatabaseEntry oldEntry = mustGetByUUID(newEntry.getUUID());
        _entries.set(_entries.indexOf(oldEntry), newEntry);
    }

    public void swap(DatabaseEntry entry1, DatabaseEntry entry2) {
        Collections.swap(_entries, _entries.indexOf(entry1), _entries.indexOf(entry2));
    }

    public List<DatabaseEntry> getList() {
        return Collections.unmodifiableList(_entries);
    }

    public DatabaseEntry getByUUID(UUID uuid) {
        for (DatabaseEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    private DatabaseEntry mustGetByUUID(UUID uuid) {
        DatabaseEntry entry = getByUUID(uuid);
        if (entry == null) {
            throw new AssertionError("no entry found with the same uuid");
        }
        return entry;
    }
}
