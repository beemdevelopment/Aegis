package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.db.DatabaseEntry;

import java.util.List;

public interface DatabaseImporter {
    void parse() throws DatabaseImporterException;
    List<DatabaseEntry> convert() throws DatabaseImporterException;
    boolean isEncrypted();
    Context getContext();
}
