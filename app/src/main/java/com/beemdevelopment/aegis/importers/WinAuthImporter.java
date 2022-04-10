package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import java.io.InputStream;

public class WinAuthImporter extends DatabaseImporter {
    public WinAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WinAuthImporter.State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        GoogleAuthUriImporter importer = new GoogleAuthUriImporter(requireContext());
        DatabaseImporter.State state = importer.read(stream);
        return new State(state);
    }

    public static class State extends DatabaseImporter.State {
        private DatabaseImporter.State _state;

        private State(DatabaseImporter.State state) {
            super(false);
            _state = state;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = _state.convert();

            for (VaultEntry entry : result.getEntries()) {
                entry.setIssuer(entry.getName());
                entry.setName("WinAuth");
            }

            return result;
        }
    }
}
