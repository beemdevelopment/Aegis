package me.impy.aegis.ext;

import java.util.List;

import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.util.ByteInputStream;

public class AegisImporter extends DatabaseImporter {

    public AegisImporter(ByteInputStream stream) {
        super(stream);
    }

    @Override
    public List<DatabaseEntry> convert() throws Exception {
        byte[] bytes = _stream.getBytes();
        Database db = new Database();
        db.deserialize(bytes);
        return db.getKeys();
    }

    @Override
    public String getName() {
        return "Aegis";
    }
}
