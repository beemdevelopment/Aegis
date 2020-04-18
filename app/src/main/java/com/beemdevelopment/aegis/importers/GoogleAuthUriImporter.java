package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GoogleAuthUriImporter extends DatabaseImporter {
    public GoogleAuthUriImporter(Context context) {
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
    public GoogleAuthUriImporter.State read(DatabaseImporter.FileReader reader) throws DatabaseImporterException {
        ArrayList<String> lines = new ArrayList<>();

        try (InputStreamReader streamReader = new InputStreamReader(reader.getStream());
             BufferedReader bufferedReader = new BufferedReader(streamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        return new GoogleAuthUriImporter.State(lines);
    }

    public static class State extends DatabaseImporter.State {
        private ArrayList<String> _lines;

        private State(ArrayList<String> lines) {
            super(false);
            _lines = lines;
        }

        @Override
        public DatabaseImporter.Result convert() {
            DatabaseImporter.Result result = new DatabaseImporter.Result();

            for (String line : _lines) {
                try {
                    VaultEntry entry = convertEntry(line);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(String line) throws DatabaseImporterEntryException {
            try {
                GoogleAuthInfo info = GoogleAuthInfo.parseUri(line);
                return new VaultEntry(info);
            } catch (GoogleAuthInfoException e) {
                throw new DatabaseImporterEntryException(e, line);
            }
        }
    }
}
