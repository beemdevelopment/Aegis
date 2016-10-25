package me.impy.aegis.db;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.encoding.Base32;

public class DatabaseHelper extends SQLiteOpenHelper {
    // NOTE: increment this every time the schema is changed
    public static final int Version = 1;

    private static final String queryCreateOTPTable =
        "create table otp (" +
        "id integer primary key autoincrement, " +
        "name varchar not null, " +
        "url varchar not null, " +
        "'order' integer)";

    public DatabaseHelper(Context context, String filename) {
        super(context, filename, null, Version);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(queryCreateOTPTable);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL(SQL_DELETE_ENTRIES);
        //onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
