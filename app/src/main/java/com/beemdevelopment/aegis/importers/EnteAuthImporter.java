package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.util.IOUtils;
import com.topjohnwu.superuser.io.SuFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EnteAuthImporter extends DatabaseImporter {
    public EnteAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            byte[] bytes = IOUtils.readAll(stream);
            GoogleAuthUriImporter importer = new GoogleAuthUriImporter(requireContext());
            return importer.read(new ByteArrayInputStream(bytes), isInternal);
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
    }
}
