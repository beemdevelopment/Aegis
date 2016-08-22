package me.impy.aegis.db;

import android.content.Context;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.KeyProfile;
import me.impy.aegis.crypto.KeyInfo;

public class Database {
    private static Database instance;
    private static Boolean libsLoaded = false;
    private SQLiteDatabase db;

    private Database(Context context, String filename, char[] password) {
        DatabaseHelper helper = new DatabaseHelper(context, filename);
        db = helper.getWritableDatabase(password);
    }

    public static Database createInstance(Context context, String filename, char[] password) {
        // load the sqlcipher library, once
        if (!libsLoaded) {
            SQLiteDatabase.loadLibs(context);
            libsLoaded = true;
        }

        if (instance == null) {
            instance = new Database(context, filename, password);
        }

        return instance;
    }

    // adds a key to the database and returns it's ID
    public void addKey(KeyProfile profile) throws Exception {
        db.execSQL("insert into otp (name, url) values (?, ?)",
                new Object[]{ profile.Name, profile.Info.getURL() });
        profile.ID = getLastID(db, "otp");
    }

    public void updateKey(KeyProfile profile) throws Exception {
        db.execSQL("update otp set name=? url=? where id=?",
                new Object[]{ profile.Name, profile.Info.getURL(), profile.ID });
    }

    public void removeKey(KeyProfile profile) {
        db.execSQL("delete from otp where id=?", new Object[]{ profile.ID });
    }

    public List<KeyProfile> getKeys() throws Exception {
        List<KeyProfile> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from otp", null);

        try {
            while (cursor.moveToNext()) {
                KeyProfile profile = new KeyProfile();
                profile.ID = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                profile.Name = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                String url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                profile.Info = KeyInfo.FromURL(url);

                list.add(profile);
            }
            return list;
        } finally {
            cursor.close();
        }
    }

    public void close() {
        db.close();
    }

    private int getLastID(SQLiteDatabase db, String table) throws Exception {
        Cursor cursor = db.rawQuery(String.format("select id from %s order by id desc limit 1", table), null);
        try {
            if (!cursor.moveToFirst()) {
                throw new Exception("no items in the table, this should not happen here");
            }

            return cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        } finally {
            cursor.close();
        }
    }
}
