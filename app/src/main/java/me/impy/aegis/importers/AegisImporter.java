package me.impy.aegis.importers;

import java.util.List;

import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseException;
import me.impy.aegis.db.DatabaseFile;
import me.impy.aegis.db.DatabaseFileException;
import me.impy.aegis.util.ByteInputStream;

public class AegisImporter extends DatabaseImporter {

    public AegisImporter(ByteInputStream stream) {
        super(stream);
    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        try {
            byte[] bytes = _stream.getBytes();
            DatabaseFile file = new DatabaseFile();
            file.deserialize(bytes);
            Database db = new Database();
            db.deserialize(file.getContent());
            return db.getKeys();
        } catch (DatabaseFileException | DatabaseException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public String getName() {
        return "Aegis";
    }
}
