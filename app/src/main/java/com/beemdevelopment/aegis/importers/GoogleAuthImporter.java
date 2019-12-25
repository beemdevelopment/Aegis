package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.topjohnwu.superuser.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class GoogleAuthImporter extends DatabaseImporter {
    private static final int TYPE_TOTP = 0;
    private static final int TYPE_HOTP = 1;

    private static final String _subPath = "databases/databases";
    private static final String _pkgName = "com.google.android.apps.authenticator2";

    public GoogleAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        return _subPath;
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        File file;

        try {
            // create a temporary copy of the database so that SQLiteDatabase can open it
            file = File.createTempFile("google-import-", "", getContext().getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                ShellUtils.pump(reader.getStream(), out);
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, OPEN_READONLY, null)) {
            try (Cursor cursor = db.rawQuery("SELECT * FROM accounts", null)) {
                List<Entry> entries = new ArrayList<>();

                if (cursor.moveToFirst()) {
                    do {
                        Entry entry = new Entry(cursor);
                        entries.add(entry);
                    } while(cursor.moveToNext());
                }

                return new State(entries);
            }
        } catch (SQLiteException e) {
            throw new DatabaseImporterException(e);
        } finally {
            // always delete the temporary file
            file.delete();
        }
    }

    public static class State extends DatabaseImporter.State {
        private List<Entry> _entries;

        private State(List<Entry> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (Entry sqlEntry : _entries) {
                try {
                    VaultEntry entry = convertEntry(sqlEntry);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(Entry entry) throws DatabaseImporterEntryException {
            try {
                byte[] secret = Base32.decode(entry.getSecret().toCharArray());

                OtpInfo info;
                switch (entry.getType()) {
                    case TYPE_TOTP:
                        info = new TotpInfo(secret);
                        break;
                    case TYPE_HOTP:
                        info = new HotpInfo(secret, entry.getCounter());
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + entry.getType());
                }

                String name = entry.getEmail();
                String[] parts = name.split(":");
                if (parts.length == 2) {
                    name = parts[1];
                }

                return new VaultEntry(info, name, entry.getIssuer());
            } catch (Base32Exception | OtpInfoException | DatabaseImporterException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }
    }

    private static String getString(Cursor cursor, String columnName) {
        return getString(cursor, columnName, null);
    }

    private static String getString(Cursor cursor, String columnName, String def) {
        String res = cursor.getString(cursor.getColumnIndex(columnName));
        if (res == null) {
            return def;
        }
        return res;
    }

    private static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    private static long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    private static class Entry {
        private int _type;
        private String _secret;
        private String _email;
        private String _issuer;
        private long _counter;

        public Entry(Cursor cursor) {
            _type = getInt(cursor, "type");
            _secret = getString(cursor, "secret");
            _email = getString(cursor, "email", "");
            _issuer = getString(cursor, "issuer", "");
            _counter = getLong(cursor, "counter");
        }

        public int getType() {
            return _type;
        }

        public String getSecret() {
            return _secret;
        }

        public String getEmail() {
            return _email;
        }

        public String getIssuer() {
            return _issuer;
        }

        public long getCounter() {
            return _counter;
        }
    }
}
