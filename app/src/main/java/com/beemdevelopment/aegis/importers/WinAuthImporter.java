package com.beemdevelopment.aegis.importers;

import android.content.Context;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

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
        ArrayList<String> lines = new ArrayList<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(reader.getStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        return new State(lines);
    }

    public static class State extends DatabaseImporter.State {
        private ArrayList<String> _lines;

        private State(ArrayList<String> lines) {
            super(false);
            _lines = lines;
        }

        @Override
        public Result convert() {
            Result result = new Result();

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
                VaultEntry vaultEntry = new VaultEntry(info);
                vaultEntry.setIssuer(vaultEntry.getName());
                vaultEntry.setName("WinAuth");

                return vaultEntry;
            } catch (GoogleAuthInfoException e) {
                throw new DatabaseImporterEntryException(e, line);
            }
        }
    }
}
