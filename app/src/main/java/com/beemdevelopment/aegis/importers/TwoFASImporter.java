package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TwoFASImporter extends DatabaseImporter {
    public TwoFASImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            String json = new String(IOUtils.readAll(stream), StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            int version = obj.getInt("schemaVersion");
            if (version > 1) {
                throw new DatabaseImporterException(String.format("Unsupported schema version: %d", version));
            }

            JSONArray array = obj.getJSONArray("services");
            List<JSONObject> entries = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                entries.add(array.getJSONObject(i));
            }

            return new TwoFASImporter.State(entries);
        } catch (IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private final List<JSONObject> _entries;

        public State(List<JSONObject> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (JSONObject obj : _entries) {
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
                byte[] secret = Base32.decode(obj.getString("secret"));
                JSONObject info = obj.getJSONObject("otp");
                String issuer = info.getString("issuer");
                String name = info.optString("account");

                OtpInfo otp = new TotpInfo(secret);
                return new VaultEntry(otp, name, issuer);
            } catch (OtpInfoException | JSONException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
