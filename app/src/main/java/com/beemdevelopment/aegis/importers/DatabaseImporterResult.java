package com.beemdevelopment.aegis.importers;

import com.beemdevelopment.aegis.db.DatabaseEntry;

import java.util.ArrayList;
import java.util.List;

public class DatabaseImporterResult {
    private List<DatabaseEntry> _entries = new ArrayList<>();
    private List<DatabaseImporterEntryException> _errors = new ArrayList<>();

    public void addEntry(DatabaseEntry entry) {
        _entries.add(entry);
    }

    public void addError(DatabaseImporterEntryException error) {
        _errors.add(error);
    }

    public List<DatabaseEntry> getEntries() {
        return _entries;
    }

    public List<DatabaseImporterEntryException> getErrors() {
        return _errors;
    }
}
