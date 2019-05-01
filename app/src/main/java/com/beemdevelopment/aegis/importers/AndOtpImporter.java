package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AndOtpImporter extends DatabaseImporter {

    public AndOtpImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    public State read(FileReader reader) throws DatabaseImporterException {
        byte[] bytes;
        try {
            bytes = reader.readAll();
            JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            return new State(array);
        } catch (IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private JSONArray _obj;

        private State(JSONArray obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            for (int i = 0; i < _obj.length(); i++) {
                try {
                    JSONObject obj = _obj.getJSONObject(i);
                    DatabaseEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (JSONException e) {
                    throw new DatabaseImporterException(e);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static DatabaseEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algorithm");
                int digits = obj.getInt("digits");
                byte[] secret = Base32.decode(obj.getString("secret").toCharArray());

                OtpInfo info;
                switch (type) {
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    case "totp":
                        info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                        break;
                    case "steam":
                        info = new SteamInfo(secret, algo, digits, obj.optInt("period", 30));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                String name;
                String issuer = "";

                String[] parts = obj.getString("label").split(" - ");
                if (parts.length > 1) {
                    issuer = parts[0];
                    name = parts[1];
                } else {
                    name = parts[0];
                }

                return new DatabaseEntry(info, name, issuer);
            } catch (DatabaseImporterException | Base32Exception | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
