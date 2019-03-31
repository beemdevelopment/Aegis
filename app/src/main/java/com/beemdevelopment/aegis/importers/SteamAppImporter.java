package com.beemdevelopment.aegis.importers;

import android.annotation.SuppressLint;
import android.content.Context;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.Base64Exception;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.util.ByteInputStream;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SteamAppImporter extends DatabaseAppImporter {
    @SuppressLint("SdCardPath")
    private static final String _path = "/data/data/com.valvesoftware.android.steam.community/files";

    private List<JSONObject> _objs = new ArrayList<>();

    public SteamAppImporter(Context context) {
        super(context);
    }

    @Override
    public void parse() throws DatabaseImporterException {
        SuFile dir = new SuFile(_path);
        for (SuFile file : dir.listFiles((d, name) -> name.startsWith("Steamguard-"))) {
            try (SuFileInputStream in = new SuFileInputStream(file)) {
                try (ByteInputStream stream = ByteInputStream.create(in)) {
                    JSONObject obj = new JSONObject(new String(stream.getBytes(), StandardCharsets.UTF_8));
                    _objs.add(obj);
                }
            } catch (IOException | JSONException e) {
                throw new DatabaseImporterException(e);
            }
        }
    }

    @Override
    public DatabaseImporterResult convert() {
        DatabaseImporterResult result = new DatabaseImporterResult();

        for (JSONObject obj : _objs) {
            try {
                DatabaseEntry entry = convertEntry(obj);
                result.addEntry(entry);
            } catch (DatabaseImporterEntryException e) {
                result.addError(e);
            }
        }

        return result;
    }

    public DatabaseEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException{
        try {
            byte[] secret = Base64.decode(obj.getString("shared_secret"));
            SteamInfo info = new SteamInfo(secret);

            String account = obj.getString("account_name");
            return new DatabaseEntry(info, account, "Steam");
        } catch (JSONException | Base64Exception | OtpInfoException e) {
            throw new DatabaseImporterEntryException(e, obj.toString());
        }
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }
}
