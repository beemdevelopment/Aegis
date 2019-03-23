package com.beemdevelopment.aegis.importers;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
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
            _obj = new JSONArray(new String(_stream.getBytes(), "UTF-8"));
        } catch (JSONException e) {
            throw new DatabaseImporterException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        List<DatabaseEntry> entries = new ArrayList<>();

        try {
            for (int i = 0; i < _obj.length(); i++) {
                JSONObject obj = _obj.getJSONObject(i);

                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algorithm");
                int digits = obj.getInt("digits");
                byte[] secret = Base32.decode(obj.getString("secret").toCharArray());

                OtpInfo info;
                if (type.equals("totp")) {
                    info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                } else if (type.equals("hotp")) {
                    info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                } else {
                    throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                String issuer = "";
                String name = "";

                String[] parts = obj.getString("label").split(" - ");
                if (parts.length > 1) {
                    issuer = parts[0];
                    name = parts[1];
                } else {
                    name = parts[0];
                }

                DatabaseEntry entry = new DatabaseEntry(info, name, issuer);
                entries.add(entry);
            }
        } catch (Base32Exception | OtpInfoException | JSONException e) {
            throw new DatabaseImporterException(e);
        }

        return entries;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }
}
