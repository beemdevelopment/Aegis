package com.beemdevelopment.aegis.importers;

import android.content.Context;

public interface DatabaseImporter {
    void parse() throws DatabaseImporterException;
    DatabaseImporterResult convert() throws DatabaseImporterException;
    boolean isEncrypted();
    Context getContext();
}
