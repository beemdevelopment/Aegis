package me.impy.aegis.importers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.KeyInfoException;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.encoding.Base32;
import me.impy.aegis.encoding.Base32Exception;
import me.impy.aegis.util.ByteInputStream;

public class AndOTPImporter extends DatabaseImporter {
    private JSONArray _obj;

    public AndOTPImporter(ByteInputStream stream) {
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

                KeyInfo key = new KeyInfo();
                key.setAlgorithm(obj.getString("algorithm"));
                key.setDigits(obj.getInt("digits"));
                key.setPeriod(obj.getInt("period"));
                key.setType(obj.getString("type"));
                if (key.getType().equals("hotp")) {
                    key.setCounter(obj.getLong("counter"));
                }

                String[] parts = obj.getString("label").split(" - ");
                if (parts.length > 1) {
                    key.setIssuer(parts[0]);
                    key.setAccountName(parts[1]);
                } else {
                    key.setAccountName(parts[0]);
                }

                byte[] secret = Base32.decode(obj.getString("secret").toCharArray());
                key.setSecret(secret);

                DatabaseEntry entry = new DatabaseEntry(key);
                entries.add(entry);
            }
        } catch (Base32Exception | KeyInfoException | JSONException e) {
            throw new DatabaseImporterException(e);
        }

        return entries;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public String getName() {
        return "andOTP";
    }
}
