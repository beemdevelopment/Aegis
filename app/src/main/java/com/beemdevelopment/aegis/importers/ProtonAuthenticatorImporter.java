package com.beemdevelopment.aegis.importers;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class ProtonAuthenticatorImporter extends DatabaseImporter {

    public ProtonAuthenticatorImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected @NonNull State read(@NonNull InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            String contents = new String(IOUtils.readAll(stream), UTF_8);
            JSONObject json = new JSONObject(contents);

            return new DecryptedState(json);
        } catch (JSONException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class DecryptedState extends DatabaseImporter.State {
        private final JSONObject _json;

        public DecryptedState(@NonNull JSONObject json) {
            super(false);
            _json = json;
        }

        @Override
        public @NonNull Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                JSONArray entries = _json.getJSONArray("entries");
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    try {
                        result.addEntry(convertEntry(entry));
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
                throw new DatabaseImporterException(e);
            }

            return result;
        }

        private static @NonNull VaultEntry convertEntry(@NonNull JSONObject entry) throws DatabaseImporterEntryException {
            try {
                JSONObject content = entry.getJSONObject("content");
                String name = content.getString("name");
                String uriString = content.getString("uri");

                Uri uri = Uri.parse(uriString);
                try {
                    GoogleAuthInfo info = GoogleAuthInfo.parseUri(uri);
                    OtpInfo otp = info.getOtpInfo();

                    return new VaultEntry(otp, name, info.getIssuer());
                } catch (GoogleAuthInfoException e) {
                    throw new DatabaseImporterEntryException(e, uriString);
                }
            } catch (JSONException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }
    }
}