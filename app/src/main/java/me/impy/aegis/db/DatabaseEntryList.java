package me.impy.aegis.db;

import android.support.annotation.NonNull;

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
        if (tryGetByUUID(entry.getUUID()) != null) {
            throw new AssertionError("entry found with the same uuid");
        }
        _entries.add(entry);
    }

    public void remove(DatabaseEntry entry) {
        entry = getByUUID(entry.getUUID());
        _entries.remove(entry);
    }

    public void replace(DatabaseEntry newEntry) {
        DatabaseEntry oldEntry = getByUUID(newEntry.getUUID());
        _entries.set(_entries.indexOf(oldEntry), newEntry);
    }

    public void swap(DatabaseEntry entry1, DatabaseEntry entry2) {
        Collections.swap(_entries, _entries.indexOf(entry1), _entries.indexOf(entry2));
    }

    public List<DatabaseEntry> getList() {
        return Collections.unmodifiableList(_entries);
    }

    private DatabaseEntry tryGetByUUID(UUID uuid) {
        for (DatabaseEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    private DatabaseEntry getByUUID(UUID uuid) {
        DatabaseEntry entry = tryGetByUUID(uuid);
        if (entry == null) {
            throw new AssertionError("no entry found with the same uuid");
        }
        return entry;
    }
}
