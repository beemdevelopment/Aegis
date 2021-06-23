package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Xml;

import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.PreferenceParser;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FreeOtpImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs/tokens.xml";
    private static final String _pkgName = "org.fedorahosted.freeotp";

    public FreeOtpImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() throws PackageManager.NameNotFoundException {
        return getAppPath(_pkgName, _subPath);
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            List<JSONObject> entries = new ArrayList<>();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (!entry.Name.equals("tokenOrder")) {
                    entries.add(new JSONObject(entry.Value));
                }
            }
            return new State(entries);
        } catch (XmlPullParserException | IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private List<JSONObject> _entries;

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
                String type = obj.getString("type").toLowerCase(Locale.ROOT);
                String algo = obj.getString("algo");
                int digits = obj.getInt("digits");
                byte[] secret = toBytes(obj.getJSONArray("secret"));

                String issuer = obj.getString("issuerExt");
                String name = obj.optString("label");

                OtpInfo info;
                switch (type) {
                    case "totp":
                        int period = obj.getInt("period");
                        if (issuer.equals("Steam")) {
                            info = new SteamInfo(secret, algo, digits, period);
                        } else {
                            info = new TotpInfo(secret, algo, digits, period);
                        }
                        break;
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                return new VaultEntry(info, name, issuer);
            } catch (DatabaseImporterException | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }

    private static byte[] toBytes(JSONArray array) throws JSONException {
        byte[] bytes = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            bytes[i] = (byte)array.getInt(i);
        }
        return bytes;
    }
}
