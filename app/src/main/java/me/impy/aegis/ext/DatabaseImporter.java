package me.impy.aegis.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.util.ByteInputStream;

public abstract class DatabaseImporter {
    private static List<Class<? extends DatabaseImporter>> _converters = Collections.unmodifiableList(
            new ArrayList<>(Arrays.asList(AegisImporter.class, FreeOTPImporter.class))
    );

    protected ByteInputStream _stream;

    protected DatabaseImporter(ByteInputStream stream) {
        _stream = stream;
    }

    public abstract List<DatabaseEntry> convert() throws Exception;

    public abstract String getName();

    public static DatabaseImporter create(ByteInputStream stream, Class<? extends DatabaseImporter> type) {
        try {
            return type.getConstructor(ByteInputStream.class).newInstance(stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<DatabaseImporter> create(ByteInputStream stream) {
        List<DatabaseImporter> list = new ArrayList<>();
        for (Class<? extends DatabaseImporter> type : _converters) {
            list.add(create(stream, type));
        }
        return list;
    }
}
