package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;

import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SteamImporter extends DatabaseImporter {
    private static final String _subDir = "files";
    private static final String _pkgName = "com.valvesoftware.android.steam.community";

    public SteamImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() throws DatabaseImporterException, PackageManager.NameNotFoundException {
        // NOTE: this assumes that a global root shell has already been obtained by the caller
        SuFile path = getAppPath(getAppPkgName(), _subDir);
        SuFile[] files = path.listFiles((d, name) -> name.startsWith("Steamguard-"));
        if (files == null || files.length == 0) {
            throw new DatabaseImporterException(String.format("Empty directory: %s", path.getAbsolutePath()));
        }

        // TODO: handle multiple files (can this even occur?)
        return new SuFile(_subDir, files[0].getName()).getPath();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            byte[] bytes = reader.readAll();
            JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            return new State(obj);
        } catch (IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private JSONObject _obj;

        private State(JSONObject obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            try {
                VaultEntry entry = convertEntry(_obj);
                result.addEntry(entry);
            } catch (DatabaseImporterEntryException e) {
                result.addError(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                byte[] secret = Base64.decode(obj.getString("shared_secret"));
                SteamInfo info = new SteamInfo(secret);

                String account = obj.getString("account_name");
                return new VaultEntry(info, account, "Steam");
            } catch (JSONException | EncodingException | OtpInfoException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
