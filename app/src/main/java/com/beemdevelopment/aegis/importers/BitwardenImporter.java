package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.simpleflatmapper.csv.CsvParser;
import org.simpleflatmapper.lightningcsv.Row;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BitwardenImporter extends DatabaseImporter {
    public BitwardenImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        String fileString;
        try {
            fileString = new String(IOUtils.readAll(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
        try {
            JSONObject obj = new JSONObject(fileString);
            JSONArray array = obj.getJSONArray("items");

            List<String> entries = new ArrayList<>();
            String entry;
            for (int i = 0; i < array.length(); i++) {
                entry = array.getJSONObject(i).getJSONObject("login").getString("totp");
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
            }

            return new BitwardenImporter.State(entries);
        } catch (JSONException e) {
            try {
                Iterator<Row> rowIterator = CsvParser.separator(',').rowIterator(fileString);
                List<String> entries = new ArrayList<>();
                rowIterator.forEachRemaining((row -> {
                    String entry = row.get("login_totp");
                    if (entry != null && !entry.isEmpty()) {
                        entries.add(entry);
                    }
                }));
                return new BitwardenImporter.State(entries);
            } catch (IOException e2) {
                throw new DatabaseImporterException(e2);
            }
        }
    }

    public static class State extends DatabaseImporter.State {
        private final List<String> _entries;

        public State(List<String> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (String obj : _entries) {
                try {
                    VaultEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(String obj) throws DatabaseImporterEntryException {
            try {
                GoogleAuthInfo info = BitwardenImporter.parseUri(obj);
                return new VaultEntry(info);
            } catch (GoogleAuthInfoException | EncodingException | OtpInfoException | URISyntaxException e) {
                throw new DatabaseImporterEntryException(e, obj);
            }
        }
    }

    private static GoogleAuthInfo parseUri(String s) throws EncodingException, OtpInfoException, URISyntaxException, GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (Objects.equals(uri.getScheme(), "steam")) {
            String secretString = uri.getAuthority();
            if (secretString == null) {
                throw new GoogleAuthInfoException(uri, "Empty secret (empty authority)");
            }
            byte[] secret = Base32.decode(secretString);
            return new GoogleAuthInfo(new SteamInfo(secret), "Steam account", "Steam");
        }

        return GoogleAuthInfo.parseUri(uri);
    }
}
