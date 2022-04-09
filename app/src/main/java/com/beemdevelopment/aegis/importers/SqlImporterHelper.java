package com.beemdevelopment.aegis.importers;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.beemdevelopment.aegis.util.IOUtils;
import com.google.common.io.Files;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SqlImporterHelper {
    private Context _context;

    public SqlImporterHelper(Context context) {
        _context = context;
    }

    public <T extends Entry> List<T> read(Class<T> type, SuFile path, String table) throws DatabaseImporterException {
        File dir = Files.createTempDir();
        File mainFile = new File(dir, path.getName());

        List<File> fileCopies = new ArrayList<>();
        for (SuFile file : SqlImporterHelper.findDatabaseFiles(path)) {
            // create temporary copies of the database files so that SQLiteDatabase can open them
            File fileCopy = null;
            try (InputStream inStream = SuFileInputStream.open(file)) {
                fileCopy = new File(dir, file.getName());
                try (FileOutputStream out = new FileOutputStream(fileCopy)) {
                    IOUtils.copy(inStream, out);
                }
                fileCopies.add(fileCopy);
            } catch (IOException e) {
                if (fileCopy != null) {
                    fileCopy.delete();
                }

                for (File fileCopy2 : fileCopies) {
                    fileCopy2.delete();
                }

                throw new DatabaseImporterException(e);
            }
        }

        try {
            return read(type, mainFile, table);
        } finally {
            for (File fileCopy : fileCopies) {
                fileCopy.delete();
            }
        }
    }

    private static SuFile[] findDatabaseFiles(SuFile path) throws DatabaseImporterException {
        SuFile[] files = path.getParentFile().listFiles((d, name) -> name.startsWith(path.getName()));
        if (files == null || files.length == 0) {
            throw new DatabaseImporterException(String.format("File does not exist: %s", path.getAbsolutePath()));
        }

        return files;
    }

    public <T extends Entry> List<T> read(Class<T> type, InputStream inStream, String table) throws DatabaseImporterException {
        File file = null;
        try {
            // create a temporary copy of the database so that SQLiteDatabase can open it
            file = File.createTempFile("db-import-", "", _context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                IOUtils.copy(inStream, out);
            }
        } catch (IOException e) {
            if (file != null) {
                file.delete();
            }
            throw new DatabaseImporterException(e);
        }

        try {
            return read(type, file, table);
        } finally {
            // always delete the temporary file
            file.delete();
        }
    }

    private <T extends Entry> List<T> read(Class<T> type, File file, String table) throws DatabaseImporterException {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, OPEN_READONLY)) {
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
        }
    }

    @SuppressLint("Range")
    public static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    @SuppressLint("Range")
    public static String getString(Cursor cursor, String columnName, String def) {
        String res = cursor.getString(cursor.getColumnIndex(columnName));
        if (res == null) {
            return def;
        }
        return res;
    }

    @SuppressLint("Range")
    public static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    @SuppressLint("Range")
    public static long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public static abstract class Entry {
        public Entry(Cursor cursor) {

        }
    }
}
