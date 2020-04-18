package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.vault.VaultEntry;

public class WinAuthImporter extends DatabaseImporter {
    public WinAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return null;
    }

    @Override
    protected String getAppSubPath() {
        return null;
    }

    @Override
    public WinAuthImporter.State read(FileReader reader) throws DatabaseImporterException {
        GoogleAuthUriImporter importer = new GoogleAuthUriImporter(getContext());
        GoogleAuthUriImporter.State state = importer.read(reader);
        return new State(state);
    }

    public static class State extends DatabaseImporter.State {
        private GoogleAuthUriImporter.State _state;

        private State(GoogleAuthUriImporter.State state) {
            super(false);
            _state = state;
        }

        @Override
        public Result convert() {
            Result result = _state.convert();

            for (VaultEntry entry : result.getEntries()) {
                entry.setIssuer(entry.getName());
                entry.setName("WinAuth");
            }

            return result;
        }
    }
}
