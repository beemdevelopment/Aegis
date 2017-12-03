package me.impy.aegis.ext;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseEntry;

public class AegisImporter extends KeyConverter {

    public AegisImporter(InputStream stream) {
        super(stream);
    }

    @Override
    public List<DatabaseEntry> convert() throws Exception {
        int read;
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        while ((read = _stream.read(buffer, 0, buffer.length)) != -1) {
            stream.write(buffer, 0, read);
        }

        byte[] bytes = stream.toByteArray();
        Database db = new Database();
        db.deserialize(bytes);
        return db.getKeys();
    }
}
