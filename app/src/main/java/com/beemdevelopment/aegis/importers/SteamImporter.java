package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SteamImporter extends DatabaseImporter {
    private static final String _subDir = "files";
    private static final String _pkgName = "com.valvesoftware.android.steam.community";

    public SteamImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException {
        // NOTE: this assumes that a global root shell has already been obtained by the caller
        SuFile path = getAppPath(_pkgName, _subDir);
        SuFile[] files = path.listFiles((d, name) -> name.startsWith("Steamguard-"));
        if (files == null || files.length == 0) {
            throw new DatabaseImporterException(String.format("Empty directory: %s", path.getAbsolutePath()));
        }

        // TODO: handle multiple files (can this even occur?)
        return files[0];
    }

    @Override
    public boolean isInstalledAppVersionSupported() {
        PackageInfo info;
        try {
            info = requireContext().getPackageManager().getPackageInfo(_pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return info.versionCode < 7460894;
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            byte[] bytes = IOUtils.readAll(stream);
            JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));

            List<JSONObject> objs = new ArrayList<>();
            if (obj.has("accounts")) {
                JSONObject accounts = obj.getJSONObject("accounts");
                Iterator<String> keys = accounts.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    objs.add(accounts.getJSONObject(key));
                }
            } else {
                objs.add(obj);
            }
            return new State(objs);
        } catch (IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private final List<JSONObject> _objs;

        private State(List<JSONObject> objs) {
            super(false);
            _objs = objs;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (JSONObject obj : _objs) {
                try {
                    VaultEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
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
