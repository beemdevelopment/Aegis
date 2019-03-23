package com.beemdevelopment.aegis.importers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class GoogleAuthAppImporter extends DatabaseAppImporter {
    private static final int TYPE_TOTP = 0;
    private static final int TYPE_HOTP = 1;

    @SuppressLint("SdCardPath")
    private static final String _filename = "/data/data/com.google.android.apps.authenticator2/databases/databases";

    private List<DatabaseEntry> _entries = new ArrayList<>();

    public GoogleAuthAppImporter(Context context) {
        super(context);
    }

    @Override
    public void parse() throws DatabaseImporterException {
        File file;

        try {
            // create a temporary copy of the database so that SQLiteDatabase can open it
            file = File.createTempFile("google-import-", "", getContext().getCacheDir());
            try (SuFileInputStream in = new SuFileInputStream(new SuFile(_filename))) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    ShellUtils.pump(in, out);
                }
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, OPEN_READONLY, null)) {
            try (Cursor cursor = db.rawQuery("SELECT * FROM accounts", null)) {
                if (!cursor.moveToFirst()) {
                    return;
                }

                do {
                    int type = getInt(cursor, "type");
                    byte[] secret = Base32.decode(getString(cursor, "secret").toCharArray());

                    OtpInfo info;
                    switch (type) {
                        case TYPE_TOTP:
                            info = new TotpInfo(secret);
                            break;
                        case TYPE_HOTP:
                            info = new HotpInfo(secret, getInt(cursor, "counter"));
                            break;
                        default:
                            throw new DatabaseImporterException("unsupported otp type: " + type);
                    }

                    String name = getString(cursor, "email", "");
                    String issuer = getString(cursor, "issuer", "");

                    String[] parts = name.split(":");
                    if (parts.length == 2) {
                        name = parts[1];
                    }

                    DatabaseEntry entry = new DatabaseEntry(info, name, issuer);
                    _entries.add(entry);
                } while(cursor.moveToNext());
            }
        } catch (SQLiteException | OtpInfoException | Base32Exception e) {
            throw new DatabaseImporterException(e);
        } finally {
            // always delete the temporary file
            file.delete();
        }
    }

    @Override
    public List<DatabaseEntry> convert() {
        return _entries;
    }

    @Override
    public boolean isEncrypted() {
        return false;
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
}
