package me.impy.aegis.importers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.encoding.Base32;
import me.impy.aegis.encoding.Base32Exception;
import me.impy.aegis.otp.HotpInfo;
import me.impy.aegis.otp.OtpInfo;
import me.impy.aegis.otp.OtpInfoException;
import me.impy.aegis.otp.TotpInfo;
import me.impy.aegis.util.ByteInputStream;

public class AndOtpImporter extends DatabaseImporter {
    private JSONArray _obj;

    public AndOtpImporter(ByteInputStream stream) {
        super(stream);
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

                String type = obj.getString("type");
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
