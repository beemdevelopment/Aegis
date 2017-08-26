package me.impy.aegis.ext;

import java.io.InputStream;
import java.util.List;

import me.impy.aegis.db.DatabaseEntry;

public abstract class KeyConverter {
    protected InputStream _stream;

    public KeyConverter(InputStream stream) {
        _stream = stream;
    }

    public abstract List<DatabaseEntry> convert() throws Exception;
}
