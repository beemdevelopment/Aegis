package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.database.Cursor;

import com.topjohnwu.superuser.ShellUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class SqlImporterHelper {
    private Context _context;
    private char[] _password;
    private boolean _compatibilityMode;

    public SqlImporterHelper(Context context) {
        _context = context;
        _password = null;
    }

    public SqlImporterHelper(Context context, char[] password, boolean compatibilityMode) {
        _context = context;
        _password = password;
        _compatibilityMode = compatibilityMode;
    }

    public <T extends Entry> List<T> read(Class<T> type, InputStream inStream, String table) throws DatabaseImporterException {
        File file;

        try {
            // create a temporary copy of the database so that SQLiteDatabase can open it
            file = File.createTempFile("db-import-", "", _context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                ShellUtils.pump(inStream, out);
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        SQLiteDatabase.loadLibs(_context);
        SQLiteDatabaseHook compatibilityHook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase database) {

            }

            @Override
            public void postKey(SQLiteDatabase database) {
                if (_compatibilityMode) {
                    database.compileStatement("PRAGMA cipher_compatibility = 3;");
                }
            }
        };

        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), _password, null, OPEN_READONLY, compatibilityHook)) {
            try (Cursor cursor = db.rawQuery(String.format("SELECT * FROM %s", table), null)) {
                List<T> entries = new ArrayList<>();

                if (cursor.moveToFirst()) {
                    do {
                        T entry = type.getDeclaredConstructor(Cursor.class).newInstance(cursor);
                        entries.add(entry);
                    } while (cursor.moveToNext());
                }

                return entries;
            } catch (InstantiationException | IllegalAccessException
                    | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLiteException e) {
            throw new DatabaseImporterException(e);
        } finally {
            // always delete the temporary file
            file.delete();
        }
    }

    public static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public static String getString(Cursor cursor, String columnName, String def) {
        String res = cursor.getString(cursor.getColumnIndex(columnName));
        if (res == null) {
            return def;
        }
        return res;
    }

    public static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    public static long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public static abstract class Entry {
        public Entry(Cursor cursor) {

        }
    }
}
