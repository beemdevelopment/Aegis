package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.util.ByteInputStream;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DatabaseFileImporter implements DatabaseImporter {
    private static Map<String, Class<? extends DatabaseFileImporter>> _importers;
    static {
        // note: keep this list sorted alphabetically
        LinkedHashMap<String, Class<? extends DatabaseFileImporter>> importers = new LinkedHashMap<>();
        importers.put("Aegis", AegisFileImporter.class);
        importers.put("andOTP", AndOtpFileImporter.class);
        importers.put("FreeOTP", FreeOtpFileImporter.class);
        _importers = Collections.unmodifiableMap(importers);
    }

    private Context _context;
    protected ByteInputStream _stream;

    protected DatabaseFileImporter(Context context, ByteInputStream stream) {
        _context = context;
        _stream = stream;
    }

    public abstract void parse() throws DatabaseImporterException;

    public abstract DatabaseImporterResult convert() throws DatabaseImporterException;

    public abstract boolean isEncrypted();

    public Context getContext() {
        return _context;
    }

    public static DatabaseFileImporter create(Context context, ByteInputStream stream, Class<? extends DatabaseFileImporter> type) {
        try {
            return type.getConstructor(Context.class, ByteInputStream.class).newInstance(context, stream);
        } catch (IllegalAccessException | InstantiationException
                | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Class<? extends DatabaseFileImporter>> getImporters() {
        return _importers;
    }
}
