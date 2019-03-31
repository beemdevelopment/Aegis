package com.beemdevelopment.aegis.importers;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.ByteInputStream;

public class AndOtpFileImporter extends DatabaseFileImporter {
    private JSONArray _obj;

    public AndOtpFileImporter(Context context, ByteInputStream stream) {
        super(context, stream);
    }

    @Override
    public void parse() throws DatabaseImporterException {
        try {
            _obj = new JSONArray(new String(_stream.getBytes(), StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public DatabaseImporterResult convert() throws DatabaseImporterException {
        DatabaseImporterResult result = new DatabaseImporterResult();

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
                    info = new SteamInfo(secret, algo, digits, obj.getInt("period"));
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

    @Override
    public boolean isEncrypted() {
        return false;
    }
}
